package com.runehearth.app.net

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import androidx.room.withTransaction
import com.runehearth.app.RunehearthApp
import com.runehearth.app.data.GameCharacter
import com.runehearth.app.data.Member
import com.runehearth.app.engine.Action
import com.runehearth.app.engine.ActionResult
import com.runehearth.app.engine.Actor
import com.runehearth.app.engine.EntityStore
import com.runehearth.app.engine.EntityType
import com.runehearth.app.engine.SyncRoute
import com.runehearth.app.engine.routeFor
import com.runehearth.app.session.ConnectionState
import com.runehearth.app.session.LocalSession
import com.runehearth.app.session.newEntityId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import com.runehearth.app.engine.ChangeBatch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

data class ConnectedPlayer(val deviceId: String, val name: String, val characterId: String?, val connectedAt: Long, val isDm: Boolean = false, val rttMs: Long = -1)

/** Authenticated, framed connection used by both host and client code. */
class Conn(private val socket: Socket) {
    private val input = DataInputStream(socket.getInputStream().buffered())
    private val output = DataOutputStream(socket.getOutputStream().buffered())
    private val writeLock = Mutex()
    private var key: ByteArray? = null
    private var sendCounter = 0L
    private var recvCounter = 0L

    fun authenticate(sessionKey: ByteArray) { key = sessionKey; sendCounter = 0; recvCounter = 0 }

    suspend fun send(m: Msg) = writeLock.withLock {
        val payload = WireJson.encode(m)
        withContext(Dispatchers.IO) { Frames.write(output, payload, key, sendCounter) }
        if (key != null) sendCounter++
    }

    /** Blocking read (runs on IO). Throws on EOF / timeout / bad MAC. */
    suspend fun receive(): Msg = withContext(Dispatchers.IO) {
        val f = Frames.read(input, key, recvCounter)
        if (f.authenticated) recvCounter++
        val m = WireJson.decode(f.payload)
        if (key != null && !f.authenticated && m !is Msg.Denied) throw IOException("plain frame after authentication")
        m
    }

    fun close() { runCatching { socket.close() } }
    val remote: String get() = socket.inetAddress?.hostAddress ?: "?"
}

/**
 * The co-op host: the DM's [LocalSession] plus a TCP server that validates pairing, serves incremental deltas and
 * executes remote players' actions through the same [com.runehearth.app.engine.ActionEngine] as the DM.
 */
class HostSession(app: RunehearthApp, campaignId: String, private val deviceId: String) : LocalSession(app, campaignId, deviceId) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dao get() = app.db.dao()
    private var server: ServerSocket? = null
    private var nsd: NsdManager? = null
    private var registration: NsdManager.RegistrationListener? = null
    private val clients = ConcurrentHashMap<String, ClientHandle>()
    private val dmClients = ConcurrentHashMap<String, ClientHandle>()
    private val sampler = MetricsSampler()
    @Volatile private var syncMsAvg = 0.0
    private val failures = ArrayDeque<Long>()
    private val rng = SecureRandom()

    private val _code = MutableStateFlow("")
    val code: StateFlow<String> = _code
    private val _dmCode = MutableStateFlow("")
    /** Pairing code for a Dungeon Master controller device (the DM running the game from a different phone than the host). */
    val dmCode: StateFlow<String> = _dmCode
    private val _metrics = MutableStateFlow(HostMetrics())
    val metrics: StateFlow<HostMetrics> = _metrics
    private val _address = MutableStateFlow("")
    val address: StateFlow<String> = _address
    private val _players = MutableStateFlow<List<ConnectedPlayer>>(emptyList())
    val players: StateFlow<List<ConnectedPlayer>> = _players
    private val _hosting = MutableStateFlow(false)
    val hosting: StateFlow<Boolean> = _hosting
    private var broadcastJob: Job? = null

    private class ClientHandle(val deviceId: String, val name: String, val conn: Conn, val job: Job, val isDm: Boolean = false) {
        @Volatile var characterId: String? = null
        @Volatile var rttMs: Long = -1
        val connectedAt = System.currentTimeMillis()
    }

    /** Opens the server socket, registers the mDNS service and starts broadcasting changes. */
    fun startHosting(): Boolean {
        if (_hosting.value) return true
        val ss = try { ServerSocket().apply { reuseAddress = true; bind(InetSocketAddress(0)) } } catch (e: IOException) { return false }
        server = ss
        _code.value = "%06d".format(rng.nextInt(1_000_000))
        _dmCode.value = newDistinctCode(_code.value)
        _address.value = "${NetUtil.localAddress() ?: "?"}:${ss.localPort}"
        _hosting.value = true
        state.value = ConnectionState.HOSTING
        scope.launch { acceptLoop(ss) }
        broadcastJob = scope.launch { app.engine.changes.collect { batch -> if (batch.campaignId == campaignId) { broadcast(batch.changes); broadcastDm(batch) } } }
        scope.launch { registerService(ss.localPort) }
        scope.launch { while (isActive) { delay(2000); sampleMetrics() } }
        return true
    }

    private fun newDistinctCode(other: String): String {
        var c: String
        do { c = "%06d".format(rng.nextInt(1_000_000)) } while (c == other)
        return c
    }

    fun regenerateCode() { _code.value = newDistinctCode(_dmCode.value) }

    private fun sampleMetrics() {
        val (avg, max, n) = app.engine.stats.snapshot()
        val worst = (clients.values + dmClients.values).maxOfOrNull { it.rttMs } ?: -1L
        _metrics.value = sampler.sample(avg, max, n, syncMsAvg, clients.size + dmClients.size, worst)
    }

    fun stopHosting() {
        if (!_hosting.value) return
        _hosting.value = false
        state.value = ConnectionState.LOCAL
        broadcastJob?.cancel()
        runCatching { server?.close() }
        server = null
        unregisterService()
        (clients.values + dmClients.values).forEach { it.conn.close(); it.job.cancel() }
        clients.clear(); dmClients.clear()
        _players.value = emptyList()
    }

    override fun close() {
        stopHosting()
        scope.cancel()
    }

    // ---------- discovery
    private suspend fun registerService(port: Int) {
        val camp = dao.campaign(campaignId) ?: return
        val mgr = app.getSystemService(android.content.Context.NSD_SERVICE) as? NsdManager ?: return
        val info = NsdServiceInfo().apply {
            serviceName = "Runehearth ${camp.name.take(24)}"
            serviceType = SERVICE_TYPE
            setPort(port)
            setAttribute("cid", camp.id); setAttribute("name", camp.name.take(40)); setAttribute("ep", camp.epoch.toString())
        }
        val l = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(i: NsdServiceInfo) {}
            override fun onRegistrationFailed(i: NsdServiceInfo, e: Int) {}
            override fun onServiceUnregistered(i: NsdServiceInfo) {}
            override fun onUnregistrationFailed(i: NsdServiceInfo, e: Int) {}
        }
        nsd = mgr; registration = l
        runCatching { mgr.registerService(info, NsdManager.PROTOCOL_DNS_SD, l) }
    }

    private fun unregisterService() {
        val l = registration ?: return
        runCatching { nsd?.unregisterService(l) }
        registration = null
    }

    // ---------- server
    private suspend fun acceptLoop(ss: ServerSocket) {
        while (scope.isActive && !ss.isClosed) {
            val sock = try { withContext(Dispatchers.IO) { ss.accept() } } catch (e: IOException) { break }
            sock.tcpNoDelay = true
            sock.soTimeout = 20_000
            scope.launch { handle(Conn(sock)) }
        }
    }

    private fun tooManyFailures(): Boolean {
        val now = System.currentTimeMillis()
        synchronized(failures) {
            while (failures.isNotEmpty() && now - failures.first() > 60_000) failures.removeFirst()
            return failures.size >= 5
        }
    }

    private suspend fun handle(conn: Conn) {
        var handle: ClientHandle? = null
        try {
            val camp = dao.campaign(campaignId) ?: return
            val hostNonce = Base64.getEncoder().encodeToString(ByteArray(16).also { rng.nextBytes(it) })
            conn.send(Msg.Challenge(PROTOCOL_VERSION, hostNonce, camp.id, camp.name, camp.epoch))
            val auth = conn.receive() as? Msg.Auth ?: return
            if (tooManyFailures()) { conn.send(Msg.Denied("Too many attempts. Wait a minute and try again.")); return }
            val code = if (auth.role == "dm") _dmCode.value else _code.value
            val expected = Pairing.proof(code, hostNonce, auth.nonce, auth.deviceId)
            if (!Pairing.verify(expected, auth.proof)) {
                synchronized(failures) { failures.addLast(System.currentTimeMillis()) }
                conn.send(Msg.Denied("Wrong pairing code")); return
            }
            conn.authenticate(Pairing.sessionKey(code, hostNonce, auth.nonce))
            val name = auth.name.trim().take(40).ifEmpty { "Adventurer" }
            if (auth.role == "dm") { handleDm(conn, auth, name); return }

            // bind device -> character
            var member = dao.member(campaignId, auth.deviceId)
            if (member != null && dao.character(member.characterId) == null) member = null
            if (member == null) {
                var claim = auth.claimId; var newName = auth.newName
                if (claim == null && newName == null) {
                    val taken = dao.members(campaignId).map { it.characterId }.toSet()
                    val list = dao.characters(campaignId).filter { !it.isNpc && it.id !in taken }
                        .map { Claimable(it.id, it.name, "Lv ${it.level} ${it.className}") }
                    conn.send(Msg.NeedClaim(list))
                    val c = conn.receive() as? Msg.Claim ?: return
                    claim = c.claimId; newName = c.newName
                }
                member = bind(auth.deviceId, name, claim, newName)
                if (member == null) { conn.send(Msg.Denied("That character is not available")); return }
            }
            val charId = member.characterId
            handle = ClientHandle(auth.deviceId, name, conn, kotlin.coroutines.coroutineContext[Job]!!)
            handle.characterId = charId
            clients.put(auth.deviceId, handle)?.let { old -> old.conn.close(); old.job.cancel() }
            publishPlayers()
            dao.upsert(member.copy(lastSeen = System.currentTimeMillis()))

            // initial sync (client is registered first so no broadcast can be missed)
            val hostCamp = dao.campaign(campaignId)!!
            val full = auth.epoch != hostCamp.epoch
            val since = if (full) 0L else auth.lastSeq.coerceAtMost(hostCamp.lastSeq)
            conn.send(Msg.Bound(charId, hostCamp.lastSeq, hostCamp.epoch))
            sendDelta(conn, charId, since, full)
            conn.send(Msg.ActResult(ActionResult("hello", true, "synced", seq = hostCamp.lastSeq)))
            loop(conn, handle)
        } catch (_: IOException) {
        } catch (_: kotlinx.serialization.SerializationException) {
        } finally {
            handle?.let { if (clients[it.deviceId] === it) clients.remove(it.deviceId); publishPlayers() }
            conn.close()
        }
    }

    /** A Dungeon Master controller device: full-fidelity snapshot, then incremental updates; its actions run with DM rights. */
    private suspend fun handleDm(conn: Conn, auth: Msg.Auth, name: String) {
        var h: ClientHandle? = null
        try {
            h = ClientHandle(auth.deviceId, name, conn, kotlin.coroutines.coroutineContext[Job]!!, isDm = true)
            dmClients.put(auth.deviceId, h)?.let { old -> old.conn.close(); old.job.cancel() }
            publishPlayers()
            val camp = dao.campaign(campaignId)!!
            conn.send(Msg.DmBound(camp.lastSeq, camp.epoch))
            val text = app.backup.export(campaignId)
            val parts = text.chunked(400_000)
            parts.forEachIndexed { i, p -> conn.send(Msg.SnapshotPart(i, parts.size, p)) }
            conn.send(Msg.ActResult(ActionResult("hello", true, "synced", seq = camp.lastSeq)))
            val dmActor = Actor.dm(auth.deviceId)
            while (scope.isActive) {
                when (val m = conn.receive()) {
                    is Msg.Act -> conn.send(Msg.ActResult(app.engine.execute(campaignId, dmActor, m.action)))
                    is Msg.Ping -> { h.rttMs = m.rtt; conn.send(Msg.Pong(m.t)) }
                    else -> {}
                }
            }
        } catch (_: IOException) {
        } catch (_: kotlinx.serialization.SerializationException) {
        } finally {
            h?.let { if (dmClients[it.deviceId] === it) dmClients.remove(it.deviceId); publishPlayers() }
            conn.close()
        }
    }

    private suspend fun bind(deviceId: String, name: String, claimId: String?, newName: String?): Member? {
        val taken = dao.members(campaignId).map { it.characterId }.toSet()
        val characterId = when {
            claimId != null -> {
                val c = dao.character(claimId)
                if (c == null || c.campaignId != campaignId || c.isNpc || claimId in taken) return null
                claimId
            }
            newName != null -> {
                val id = newEntityId()
                val r = app.engine.execute(campaignId, Actor.dm(this.deviceId), Action.UpsertCharacter(newEntityId(), GameCharacter(id, campaignId, newName.trim().take(60).ifEmpty { name })))
                if (!r.ok) return null
                id
            }
            else -> return null
        }
        val m = Member(campaignId, deviceId, characterId, name, lastSeen = System.currentTimeMillis())
        dao.upsert(m)
        return m
    }

    private suspend fun sendDelta(conn: Conn, characterId: String, sinceSeq: Long, full: Boolean) {
        val rows = dao.changesSince(campaignId, sinceSeq)
        val wire = ArrayList<WireChange>(rows.size)
        var top = sinceSeq
        for (c in rows) {
            top = maxOf(top, c.seq)
            when (c.routeFor(characterId)) {
                SyncRoute.UPSERT -> wire += WireChange(c.entityType, c.entityId, c.seq, c.payload)
                SyncRoute.DELETE -> if (!full) wire += WireChange(c.entityType, c.entityId, c.seq, delete = true)
                SyncRoute.NONE -> {}
            }
        }
        // split to stay under the frame limit
        var i = 0
        if (wire.isEmpty()) conn.send(Msg.Delta(emptyList(), top, full))
        while (i < wire.size) {
            var bytes = 0; val chunk = ArrayList<WireChange>()
            while (i < wire.size && bytes < 600_000) { chunk += wire[i]; bytes += wire[i].payload.length + 80; i++ }
            conn.send(Msg.Delta(chunk, if (i >= wire.size) top else chunk.last().seq, full && chunk.first() === wire.first()))
        }
    }

    private suspend fun broadcastDm(batch: ChangeBatch) {
        if (dmClients.isEmpty() || batch.dirty.isEmpty()) return
        val wire = batch.dirty.map { (type, id) ->
            val snap = EntityStore.snapshot(dao, type, id, full = true)
            WireChange(type.name, id, batch.seq, snap?.payload ?: "", delete = snap == null)
        }
        for (h in dmClients.values) runCatching { h.conn.send(Msg.DmDelta(wire, batch.seq)) }
    }

    private suspend fun broadcast(changes: List<com.runehearth.app.data.Change>) {
        if (changes.isEmpty()) return
        val started = System.nanoTime()
        try { broadcastPlayers(changes) } finally {
            val ms = (System.nanoTime() - started) / 1e6
            syncMsAvg = if (syncMsAvg == 0.0) ms else syncMsAvg * 0.8 + ms * 0.2
        }
    }

    private suspend fun broadcastPlayers(changes: List<com.runehearth.app.data.Change>) {
        val top = changes.maxOf { it.seq }
        for (h in clients.values) {
            val cid = h.characterId ?: continue
            val wire = changes.mapNotNull { c ->
                when (c.routeFor(cid)) {
                    SyncRoute.UPSERT -> WireChange(c.entityType, c.entityId, c.seq, c.payload)
                    SyncRoute.DELETE -> WireChange(c.entityType, c.entityId, c.seq, delete = true)
                    SyncRoute.NONE -> null
                }
            }
            if (wire.isNotEmpty()) runCatching { h.conn.send(Msg.Delta(wire, top)) }
        }
    }

    private suspend fun loop(conn: Conn, h: ClientHandle) {
        val cid = h.characterId!!
        while (scope.isActive) {
            when (val m = conn.receive()) {
                is Msg.Act -> {
                    val result = app.engine.execute(campaignId, Actor(false, cid, h.deviceId), m.action)
                    conn.send(Msg.ActResult(result))
                }
                is Msg.Ping -> { h.rttMs = m.rtt; conn.send(Msg.Pong(m.t)) }
                is Msg.DigestReport -> {
                    val camp = dao.campaign(campaignId)
                    if (camp != null && camp.lastSeq == m.seq && EntityStore.ownedDigest(dao, cid) != m.digest) {
                        sendDelta(conn, cid, 0, full = true) // repair: authoritative resend of everything this player may see
                    }
                }
                else -> {}
            }
        }
    }

    private fun publishPlayers() {
        _players.update {
            (clients.values + dmClients.values).map { ConnectedPlayer(it.deviceId, it.name, it.characterId, it.connectedAt, it.isDm, it.rttMs) }.sortedBy { it.name }
        }
    }

    /** Drops the TCP connection without telling the client (simulates a network failure); the client reconnects on its own. */
    fun dropConnection(deviceId: String) { (clients[deviceId] ?: dmClients[deviceId])?.conn?.close() }

    /** Disconnects a player (DM tool). The device stays bound to its character and may rejoin with the code. */
    fun kick(deviceId: String) {
        (clients[deviceId] ?: dmClients[deviceId])?.let { scope.launch { runCatching { it.conn.send(Msg.Kick("Removed by the Dungeon Master")) }; it.conn.close() } }
    }
}

