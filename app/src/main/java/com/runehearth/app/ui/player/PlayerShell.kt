package com.runehearth.app.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.runehearth.app.net.RemoteSession
import com.runehearth.app.session.ConnectionState
import com.runehearth.app.ui.LocalSession
import com.runehearth.app.ui.combat.PlayerCombat
import com.runehearth.app.ui.components.LoadingRune
import com.runehearth.app.ui.components.RhIcon
import com.runehearth.app.ui.components.RhText
import com.runehearth.app.ui.components.RuneTabBar
import com.runehearth.app.ui.components.ScreenTopBar
import com.runehearth.app.ui.components.TabSpec
import com.runehearth.app.ui.components.Tag
import com.runehearth.app.ui.dice.DiceScreen
import com.runehearth.app.ui.economy.PlayerPurse
import com.runehearth.app.ui.rememberDao
import com.runehearth.app.ui.rememberFlow
import com.runehearth.app.ui.sheet.CharacterSheet
import com.runehearth.app.ui.theme.Theme

private val PLAYER_TABS = listOf(TabSpec("Hero", RhIcon.USER), TabSpec("Purse", RhIcon.COIN), TabSpec("Dice", RhIcon.DICE), TabSpec("Battle", RhIcon.SWORDS))

/** Player interface: own character sheet, purse, dice and the initiative tracker; works offline from the local mirror. */
@Composable
fun PlayerShell(campaignId: String, onExit: () -> Unit) {
    val session = LocalSession.current
    val remote = session as? RemoteSession
    val dao = rememberDao()
    val conn by session.connection.collectAsState()
    val charId by (remote?.characterIdFlow ?: kotlinx.coroutines.flow.MutableStateFlow(session.characterId)).collectAsState()
    val camp by rememberFlow(campaignId, null) { dao.observeCampaign(campaignId) }
    var tab by rememberSaveable { mutableStateOf(0) }
    val latency by (remote?.latencyMs ?: kotlinx.coroutines.flow.MutableStateFlow(-1L)).collectAsState()
    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(camp?.name ?: "Adventure", onExit, actions = {
            val (label, col) = when (conn) {
                ConnectionState.CONNECTED -> (if (latency >= 0) "Online ${latency}ms" else "Online") to Color(0xFF5CB85C)
                ConnectionState.RECONNECTING, ConnectionState.CONNECTING -> "Reconnecting…" to Color(0xFFE48A3C)
                else -> "Offline" to Color(0xFFD9453B)
            }
            Tag(label, col); Spacer(Modifier.size(10.dp))
        })
        if (conn == ConnectionState.RECONNECTING || conn == ConnectionState.DISCONNECTED) {
            com.runehearth.app.ui.components.Panel(Modifier.fillMaxWidth().padding(8.dp), glow = Color(0xFFE48A3C), padding = 8.dp) {
                RhText("The host is unreachable.", style = Theme.heading)
                RhText("Everything you committed is saved on this device and on the host. Reconnecting automatically; if the DM moves the game to another phone, use Join Game again — duplicate actions are never applied twice.", style = Theme.small)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val id = charId
            if (id == null || camp == null) Box(Modifier.fillMaxSize(), Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { LoadingRune(size = 64.dp); RhText("Synchronising with the host…", style = Theme.small) } }
            else AnimatedContent(tab, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) }, label = "pTab") { t ->
                when (t) {
                    0 -> CharacterSheet(id)
                    1 -> PlayerPurse(campaignId, id)
                    2 -> DiceScreen(campaignId, showHistoryOfAll = true)
                    else -> PlayerCombat(campaignId, id)
                }
            }
        }
        RuneTabBar(PLAYER_TABS, tab, { tab = it })
    }
}
