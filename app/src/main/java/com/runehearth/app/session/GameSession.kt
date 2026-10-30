package com.runehearth.app.session

import com.runehearth.app.RunehearthApp
import com.runehearth.app.engine.Action
import com.runehearth.app.engine.ActionResult
import com.runehearth.app.engine.Actor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

enum class ConnectionState { LOCAL, HOSTING, CONNECTING, CONNECTED, RECONNECTING, DISCONNECTED }

/**
 * What the UI talks to. A session is either authoritative ([LocalSession]: local-only mode or the co-op host) or a
 * remote player ([com.runehearth.app.net.RemoteSession]) that forwards actions to the host. Reads always come from the
 * local Room database, which is the source for the host and a synced mirror for players.
 */
interface GameSession {
    val campaignId: String
    val isDm: Boolean
    /** Player sessions: the character this device controls. */
    val characterId: String?
    val connection: StateFlow<ConnectionState>
    suspend fun submit(action: Action): ActionResult
    fun close()
}

fun newActionId(): String = UUID.randomUUID().toString()
fun newEntityId(): String = UUID.randomUUID().toString()

/** Runs actions directly on this device's engine as the Dungeon Master. */
open class LocalSession(
    protected val app: RunehearthApp,
    override val campaignId: String,
    deviceId: String,
) : GameSession {
    override val isDm = true
    override val characterId: String? = null
    protected val actor = Actor.dm(deviceId)
    protected val state = MutableStateFlow(ConnectionState.LOCAL)
    override val connection: StateFlow<ConnectionState> = state

    override suspend fun submit(action: Action): ActionResult = app.engine.execute(campaignId, actor, action)
    override fun close() {}
}
