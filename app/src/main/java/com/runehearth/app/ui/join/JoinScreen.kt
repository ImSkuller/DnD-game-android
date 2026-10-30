package com.runehearth.app.ui.join

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.runehearth.app.net.FoundHost
import com.runehearth.app.net.HostDiscovery
import com.runehearth.app.net.RemoteSession
import com.runehearth.app.session.ConnectionState
import com.runehearth.app.session.GameSession
import com.runehearth.app.ui.LocalApp
import com.runehearth.app.ui.components.BtnKind
import com.runehearth.app.ui.components.LoadingRune
import com.runehearth.app.ui.components.Panel
import com.runehearth.app.ui.components.RhIcon
import com.runehearth.app.ui.components.RhText
import com.runehearth.app.ui.components.RhTextField
import com.runehearth.app.ui.components.ScreenTopBar
import com.runehearth.app.ui.components.SectionHeader
import com.runehearth.app.ui.components.WoodButton
import com.runehearth.app.ui.theme.Theme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Find a host (mDNS or manual address), pair with the code and claim or create a character. */
@Composable
fun JoinScreen(onBack: () -> Unit, onJoined: (GameSession) -> Unit) {
    val app = LocalApp.current
    val discovery = remember { HostDiscovery(app) }
    val found by discovery.hosts.collectAsState()
    DisposableEffect(Unit) { discovery.start(); onDispose { discovery.stop() } }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var manual by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<FoundHost?>(null) }
    var session by remember { mutableStateOf<RemoteSession?>(null) }
    var newChar by remember { mutableStateOf("") }
    var asDm by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { name = app.prefs.playerName.first(); manual = app.prefs.lastHost.first() }
    // abandon an unfinished attempt when leaving the screen
    DisposableEffect(Unit) { onDispose { if (session?.characterId == null && session?.ready?.value != true) session?.close() } }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar("Join a Game", { session?.close(); onBack() })
        val s = session
        if (s == null) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Panel(Modifier.fillMaxWidth()) {
                        RhText("Join the Dungeon Master's session over Wi-Fi or their hotspot. No internet needed.", style = Theme.small)
                        Spacer(Modifier.height(8.dp))
                        RhTextField(name, { name = it.take(40) }, label = "Your name", modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        RhTextField(code, { code = it.filter { c -> c.isDigit() }.take(6) }, label = if (asDm) "6-digit DM code" else "6-digit player code", keyboard = KeyboardType.Number, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            com.runehearth.app.ui.components.Chip("I am a player", !asDm, { asDm = false })
                            com.runehearth.app.ui.components.Chip("I am the Dungeon Master", asDm, { asDm = true }, color = Color(0xFF8CCB5A))
                        }
                        if (asDm) RhText("Use this when a different phone is hosting the session. You keep full Dungeon Master control.", style = Theme.small)
                    }
                }
                item { SectionHeader("Nearby Hosts") }
                if (found.isEmpty()) item { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { LoadingRune(size = 32.dp); RhText("Searching the local network…", style = Theme.small, color = Theme.onDarkSoft) } }
                items(found, key = { it.key }) { h ->
                    Panel(Modifier.fillMaxWidth(), onClick = { selected = h; manual = "${h.host}:${h.port}" }, glow = if (selected?.key == h.key) Theme.gold else null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { RhText(h.name, style = Theme.heading, maxLines = 1); RhText("${h.host}:${h.port}", style = Theme.small) }
                            WoodButton("Join", { selected = h; manual = "${h.host}:${h.port}" }, compact = true, kind = BtnKind.GOLD)
                        }
                    }
                }
                item { SectionHeader("Or Enter the Address") }
                item {
                    Panel(Modifier.fillMaxWidth(), surface = com.runehearth.app.ui.components.Surface.STONE) {
                        RhTextField(manual, { manual = it.trim() }, label = "Host address (shown on the DM's screen, e.g. 192.168.1.5:4021)")
                        Spacer(Modifier.height(8.dp))
                        WoodButton("Connect", {
                            val parts = manual.split(':')
                            val host = parts.getOrNull(0).orEmpty(); val port = parts.getOrNull(1)?.toIntOrNull() ?: 0
                            if (host.isNotEmpty() && port in 1..65535 && code.length == 6) scope.launch {
                                app.prefs.setPlayerName(name); app.prefs.setLastHost(manual)
                                val rs = RemoteSession(app.db, app.backup, host, port, code, name.ifBlank { "Adventurer" }, app.prefs.deviceId(), asDm = asDm)
                                session = rs; rs.start()
                            }
                        }, Modifier.fillMaxWidth(), icon = RhIcon.WIFI, kind = BtnKind.GOLD, enabled = code.length == 6 && manual.contains(':'))
                    }
                }
            }
        } else ConnectingView(s, newChar, { newChar = it }, onJoined, onCancel = { s.close(); session = null })
    }
}

@Composable
private fun ConnectingView(s: RemoteSession, newChar: String, onNewChar: (String) -> Unit, onJoined: (GameSession) -> Unit, onCancel: () -> Unit) {
    val claimables by s.claimables.collectAsState()
    val error by s.error.collectAsState()
    val charId by s.characterIdFlow.collectAsState()
    val ready by s.ready.collectAsState()
    val state by s.connection.collectAsState()
    // wait until the first sync produced the campaign mirror, then enter
    LaunchedEffect(charId) { if (charId != null) { kotlinx.coroutines.delay(350); onJoined(s) } }
    LaunchedEffect(ready) { if (ready && s.isDm) onJoined(s) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (error != null) {
            RhText(error ?: "", style = Theme.heading, color = Color(0xFFE5534B))
            WoodButton("Back", onCancel, kind = BtnKind.SECONDARY)
        } else if (claimables != null) {
            RhText("Who are you in this tale?", style = Theme.heading)
            claimables!!.forEach { c ->
                Panel(Modifier.fillMaxWidth(), onClick = { s.claim(c.id, null) }) { RhText(c.name, style = Theme.heading); RhText(c.summary, style = Theme.small) }
            }
            Panel(Modifier.fillMaxWidth(), surface = com.runehearth.app.ui.components.Surface.PARCHMENT) {
                RhText("Or forge a new character", style = Theme.heading)
                RhTextField(newChar, onNewChar, label = "Character name", onDark = false)
                Spacer(Modifier.height(6.dp))
                WoodButton("Create & join", { s.claim(null, newChar) }, Modifier.fillMaxWidth(), kind = BtnKind.GOLD, compact = true, enabled = newChar.isNotBlank())
            }
        } else {
            LoadingRune(size = 72.dp)
            RhText(if (state == ConnectionState.CONNECTED) "Synchronising…" else "Connecting to the host…", style = Theme.heading)
            WoodButton("Cancel", onCancel, kind = BtnKind.SECONDARY, compact = true)
        }
    }
}
