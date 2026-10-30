package com.runehearth.app.ui.dm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.runehearth.app.net.HostSession
import com.runehearth.app.ui.components.BtnKind
import com.runehearth.app.ui.components.Panel
import com.runehearth.app.ui.components.RhIcon
import com.runehearth.app.ui.components.RhText
import com.runehearth.app.ui.components.ScreenTopBar
import com.runehearth.app.ui.components.WoodButton
import com.runehearth.app.ui.rememberDao
import com.runehearth.app.ui.rememberFlow
import com.runehearth.app.ui.theme.LocalPerf
import com.runehearth.app.ui.theme.PerfSettings
import com.runehearth.app.ui.theme.Theme

/**
 * Dedicated host mode: this phone only runs the authoritative engine, database and network. It renders a static,
 * animation-free status page (forced LITE rendering) so a low-end phone can host reliably while the Dungeon Master
 * plays from a different device (join with the DM code).
 */
@Composable
fun HostConsole(host: HostSession, onExit: () -> Unit) {
    CompositionLocalProvider(LocalPerf provides PerfSettings.LITE) {
        val dao = rememberDao()
        val camp by rememberFlow(host.campaignId, null) { dao.observeCampaign(host.campaignId) }
        val hosting by host.hosting.collectAsState()
        val code by host.code.collectAsState()
        val dmCode by host.dmCode.collectAsState()
        val address by host.address.collectAsState()
        val players by host.players.collectAsState()
        val metrics by host.metrics.collectAsState()
        LaunchedEffect(Unit) { if (!host.hosting.value) host.startHosting() }
        Column(Modifier.fillMaxSize()) {
            ScreenTopBar("Host: ${camp?.name ?: ""}", onExit)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Panel(Modifier.fillMaxWidth()) {
                    RhText(if (hosting) "Hosting — the game is live" else "Session closed", style = Theme.heading)
                    RhText("Players enter the PLAYER code. The Dungeon Master joins from their own phone with Join Game → \"I am the Dungeon Master\" and the DM code. Keep this screen open and the phone plugged in.", style = Theme.small)
                    Spacer(Modifier.height(8.dp))
                    if (hosting) {
                        CodesRow(code, dmCode, address)
                        Spacer(Modifier.height(8.dp))
                        WoodButton("New player code", { host.regenerateCode() }, compact = true, kind = BtnKind.SECONDARY)
                        Spacer(Modifier.height(4.dp))
                        WoodButton("Close session", { host.stopHosting() }, compact = true, kind = BtnKind.DANGER)
                    } else WoodButton("Open session", { host.startHosting() }, Modifier.fillMaxWidth(), icon = RhIcon.WIFI, kind = BtnKind.GOLD, compact = true)
                }
                Panel(Modifier.fillMaxWidth()) { ConnectedList(players, host) }
                Panel(Modifier.fillMaxWidth()) { HostMetricsView(metrics) }
            }
        }
    }
}
