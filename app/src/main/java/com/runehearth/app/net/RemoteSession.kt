package com.runehearth.app.net

import androidx.room.withTransaction
import com.runehearth.app.data.Backup
import com.runehearth.app.data.GameDatabase
import com.runehearth.app.engine.Action
import com.runehearth.app.engine.ActionResult
import com.runehearth.app.engine.EntityStore
import com.runehearth.app.engine.EntityType
import com.runehearth.app.session.ConnectionState
import com.runehearth.app.session.GameSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

private class Denial(message: String) : Exception(message)

/**
 * A player's device. Forwards actions to the host, applies incoming deltas to the local mirror database and
 * reconnects with exponential back-off, re-sending unacknowledged actions (the host de-duplicates by action id).
 */
class RemoteSession(
    private val db: GameDatabase,
    private val backup: Backup,
    private val host: String,
    private val port: Int,
    private val code: String,
    private val playerName: String,
    private val deviceId: String,
    private val claimId: String? = null,
    private val newName: String? = null,
    /** Join as a Dungeon Master controller (full-fidelity mirror, DM permissions) instead of as a player. */
    private val asDm: Boolean = false,
) : GameSession {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dao get() = db.dao()
    private val rng = SecureRandom()
    private var job: Job? = null
    @Volatile private var conn: Conn? = null
    @Volatile private var campaign = ""
    private class Pending(val action: Action, val done: CompletableDeferred<ActionResult>)
    private val pending = ConcurrentHashMap<String, Pending>()
    private var claimWait: CompletableDeferred<Msg.Claim>? = null

    override val campaignId: String get() = campaign
    override val isDm: Boolean get() = asDm
    private val _ready = MutableStateFlow(false)
    /** DM controller: true once the full snapshot has been imported. */
    val ready: StateFlow<Boolean> = _ready
    private val snapParts = ArrayList<String>()
    private val dmBuffer = ArrayList<Msg.DmDelta>()
    private var snapshotDone = false
    private var snapshotSeq = 0L
    private val _character = MutableStateFlow<String?>(null)
    val characterIdFlow: StateFlow<String?> = _character
    override val characterId: String? get() = _character.value
    private val _state = MutableStateFlow(ConnectionState.CONNECTING)
    override val connection: StateFlow<ConnectionState> = _state
    private val _claimables = MutableStateFlow<List<Claimable>?>(null)
    val claimables: StateFlow<List<Claimable>?> = _claimables
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _latency = MutableStateFlow(-1L)
    val latencyMs: StateFlow<Long> = _latency

    fun start() {
        if (job != null) return
        job = scope.launch { runLoop() }
    }

    /** Answer to a [Msg.NeedClaim] prompt. */
    fun claim(id: String?, name: String?) {
        _claimables.value = null
        claimWait?.complete(Msg.Claim(id, name))
    }

    private suspend fun runLoop() {
        var backoff = 500L
        var everBound = false
        while (scope.isActive) {
            _state.value = if (everBound) ConnectionState.RECONNECTING else ConnectionState.CONNECTING
            try {
                val sock = Socket()
                withContext(Dispatchers.IO) { sock.connect(InetSocketAddress(host, port), 4000) }
                sock.tcpNoDelay = true; sock.soTimeout = 15_000
                val c = Conn(sock)
                conn = c
                session(c) { everBound = true; backoff = 500L }
            } catch (e: Denial) {
                _error.value = e.message; _state.value = ConnectionState.DISCONNECTED; return
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // IO / protocol error -> retry
            } finally {
                conn?.close(); conn = null
            }
            if (!scope.isActive) break
            _state.value = if (everBound) ConnectionState.RECONNECTING else ConnectionState.CONNECTING
            delay(backoff); backoff = (backoff * 2).coerceAtMost(8000L)
        }
    }

    private suspend fun session(c: Conn, onBound: () -> Unit) {
        val ch = c.receive() as? Msg.Challenge ?: throw IOException("expected challenge")
        if (ch.proto != PROTOCOL_VERSION) throw Denial("The host runs an incompatible version of Runehearth")
        campaign = ch.campaignId
        val local = dao.campaign(ch.campaignId)
        val nonce = Base64.getEncoder().encodeToString(ByteArray(16).also { rng.nextBytes(it) })
        c.send(Msg.Auth(deviceId, playerName, nonce, Pairing.proof(code, ch.nonce, nonce, deviceId), local?.lastSeq ?: 0, local?.epoch ?: ch.epoch, claimId, newName, if (asDm) "dm" else "player"))
        c.authenticate(Pairing.sessionKey(code, ch.nonce, nonce))

        coroutineScope {
            var helpers: List<Job> = emptyList()
            try {
                while (isActive) {
                    when (val m = c.receive()) {
                        is Msg.Denied -> throw Denial(m.reason)
                        is Msg.NeedClaim -> {
                            val wait = CompletableDeferred<Msg.Claim>().also { claimWait = it }
                            _claimables.value = m.claimable
                            c.send(wait.await())
                        }
                        is Msg.Bound -> {
                            _character.value = m.characterId
                            _error.value = null
                            _state.value = ConnectionState.CONNECTED
                            onBound()
                            pending.values.forEach { c.send(Msg.Act(it.action)) } // resend unacked; host de-duplicates
                            helpers = listOf(
                                launch { while (isActive) { delay(5000); c.send(Msg.Ping(System.nanoTime(), _latency.value)) } },
                                launch { while (isActive) { delay(20_000); sendDigest(c) } },
                            )
                        }
                        is Msg.Delta -> applyDelta(m)
                        is Msg.DmBound -> {
                            snapParts.clear(); dmBuffer.clear(); snapshotDone = false; snapshotSeq = m.hostSeq
                            _error.value = null
                            _state.value = ConnectionState.CONNECTED
                            onBound()
                            pending.values.forEach { c.send(Msg.Act(it.action)) }
                            helpers = listOf(launch { while (isActive) { delay(5000); c.send(Msg.Ping(System.nanoTime(), _latency.value)) } })
                        }
                        is Msg.SnapshotPart -> {
                            snapParts.add(m.data)
                            if (snapParts.size == m.total) { importSnapshot(snapParts.joinToString("")); snapParts.clear() }
                        }
                        is Msg.DmDelta -> if (snapshotDone) applyDmDelta(m) else dmBuffer.add(m)
                        is Msg.ActResult -> {
                            if (m.result.actionId == "hello") { if (!asDm) sendDigest(c) }
                            else pending.remove(m.result.actionId)?.done?.complete(m.result)
                        }
                        is Msg.Pong -> _latency.value = (System.nanoTime() - m.t) / 1_000_000
                        is Msg.Kick -> throw Denial(m.reason)
                        else -> {}
                    }
                }
            } finally {
                helpers.forEach { it.cancel() }
            }
        }
    }

    /** DM controller: replace the local mirror with the host's full snapshot, then replay any updates that arrived meanwhile. */
    private suspend fun importSnapshot(text: String) {
        val id = backup.import(text, validate = false)
        campaign = id
        dao.campaign(id)?.let { dao.upsert(it.copy(isHost = false, lastSeq = snapshotSeq)) }
        snapshotDone = true
        dmBuffer.forEach { applyDmDelta(it) }
        dmBuffer.clear()
        _ready.value = true
    }

    private suspend fun applyDmDelta(m: Msg.DmDelta) {
        val cid = campaign
        db.withTransaction {
            for (c in m.changes) {
                val type = runCatching { EntityType.valueOf(c.type) }.getOrNull() ?: continue
                EntityStore.apply(dao, type, c.id, c.payload, c.delete)
            }
            dao.campaign(cid)?.let { dao.upsert(it.copy(lastSeq = maxOf(it.lastSeq, m.toSeq))) }
        }
    }

    private suspend fun sendDigest(c: Conn) {
        val cid = _character.value ?: return
        val seq = dao.campaign(campaign)?.lastSeq ?: return
        c.send(Msg.DigestReport(seq, EntityStore.ownedDigest(dao, cid)))
    }

    /** Applies a delta to the local mirror in one transaction; idempotent, ignores already-applied sequence numbers. */
    private suspend fun applyDelta(m: Msg.Delta) {
        val cid = campaign
        db.withTransaction {
            if (m.full) backup.wipe(cid)
            var last = dao.campaign(cid)?.lastSeq ?: 0L
            for (c in m.changes) {
                if (c.seq <= last && !m.full) continue
                val type = runCatching { EntityType.valueOf(c.type) }.getOrNull() ?: continue
                EntityStore.apply(dao, type, c.id, c.payload, c.delete)
            }
            last = maxOf(last, m.toSeq)
            dao.campaign(cid)?.let { dao.upsert(it.copy(lastSeq = last)) }
        }
    }

    override suspend fun submit(action: Action): ActionResult {
        val p = Pending(action, CompletableDeferred())
        pending[action.id] = p
        val c = conn
        if (c == null || _state.value != ConnectionState.CONNECTED) {
            // keep it queued briefly: it is re-sent after reconnect if that happens soon
        } else runCatching { c.send(Msg.Act(action)) }
        val r = withTimeoutOrNull(12_000) { p.done.await() }
        pending.remove(action.id)
        return r ?: ActionResult(action.id, false, if (_state.value == ConnectionState.CONNECTED) "The host did not respond" else "Not connected to the host")
    }

    override fun close() {
        scope.cancel()
        conn?.close()
        pending.values.forEach { it.done.cancel() }
    }
}
