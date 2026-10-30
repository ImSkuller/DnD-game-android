package com.runehearth.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.runehearth.app.RunehearthApp
import com.runehearth.app.data.CampaignMode
import com.runehearth.app.net.HostSession
import com.runehearth.app.session.GameSession
import com.runehearth.app.session.LocalSession
import kotlinx.coroutines.launch

sealed interface Route {
    data object Home : Route
    data object Join : Route
    data object Settings : Route
    data object Assets : Route
    /** DM/host shell for a campaign. */
    data class Dm(val campaignId: String) : Route
    /** Player shell (co-op client). */
    data class Player(val campaignId: String) : Route
}

val LocalApp = staticCompositionLocalOf<RunehearthApp> { error("no app") }
val LocalSession = staticCompositionLocalOf<GameSession> { error("no session") }

/** Holds the navigation back-stack and the active game session across configuration changes. */
class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as RunehearthApp
    val stack = mutableStateListOf<Route>(Route.Home)
    var session: GameSession? by mutableStateOf(null)
        private set

    val current: Route get() = stack.last()

    fun push(r: Route) { stack.add(r) }
    fun pop(): Boolean {
        if (stack.size <= 1) return false
        val r = stack.removeAt(stack.lastIndex)
        if (r is Route.Dm || r is Route.Player) closeSession()
        return true
    }
    fun replaceTop(r: Route) { stack[stack.lastIndex] = r }

    fun openCampaignAsDm(campaignId: String) {
        viewModelScope.launch {
            closeSession()
            val camp = app.db.dao().campaign(campaignId) ?: return@launch
            val device = app.prefs.deviceId()
            session = if (camp.mode == CampaignMode.COOP && camp.isHost) HostSession(app, campaignId, device) else LocalSession(app, campaignId, device)
            push(Route.Dm(campaignId))
        }
    }

    fun adoptSession(s: GameSession, route: Route) {
        closeSession()
        session = s
        push(route)
    }

    fun closeSession() {
        val s = session ?: return
        session = null
        viewModelScope.launch {
            runCatching { app.backup.snapshot(app.snapshotDir, s.campaignId) }
            s.close()
        }
    }
}
