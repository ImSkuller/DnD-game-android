package com.runehearth.app.ui.dm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runehearth.app.net.ConnectedPlayer
import com.runehearth.app.net.HostAdvisor
import com.runehearth.app.net.HostMetrics
import com.runehearth.app.net.HostSession
import com.runehearth.app.ui.LocalApp
import com.runehearth.app.ui.components.BtnKind
import com.runehearth.app.ui.components.LocalToaster
import com.runehearth.app.ui.components.RhIcon
import com.runehearth.app.ui.components.RhIconButton
import com.runehearth.app.ui.components.RhText
import com.runehearth.app.ui.components.Tag
import com.runehearth.app.ui.components.WoodButton
import com.runehearth.app.ui.theme.Theme

/** Resource usage of the hosting device with plain-language recommendations. */
@Composable
fun HostMetricsView(m: HostMetrics, modifier: Modifier = Modifier) {
    val app = LocalApp.current
    val advice = HostAdvisor.advise(m, app.deviceProfile)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        RhText("HOST HEALTH", style = Theme.label.copy(fontSize = 10.sp))
        MetricLine("Memory", "${m.memUsedMb} / ${m.memMaxMb} MB (+${m.nativeMb} MB native)")
        MetricLine("CPU", "${m.cpuPercent}% of one core")
        MetricLine("Save latency", "avg ${"%.1f".format(m.dbAvgMs)} ms · worst ${"%.1f".format(m.dbMaxMs)} ms (${m.actions} actions)")
        MetricLine("Update push", "${"%.1f".format(m.syncMs)} ms to ${m.clients} device(s)")
        MetricLine("Network", if (m.worstRttMs >= 0) "worst round-trip ${m.worstRttMs} ms" else "n/a")
        if (advice.isEmpty()) RhText("This device is coping well as host.", style = Theme.small, color = Color(0xFF5CB85C))
        advice.forEach { RhText("⚠ " + HostAdvisor.text(it), style = Theme.small, color = Color(0xFFE48A3C)) }
    }
}

@Composable
private fun MetricLine(k: String, v: String) {
    Row(Modifier.fillMaxWidth()) { RhText(k, Modifier.weight(1f), style = Theme.small); RhText(v, style = Theme.small, color = Theme.onDark) }
}

@Composable
fun ConnectedList(players: List<ConnectedPlayer>, host: HostSession) {
    RhText("CONNECTED (${players.size})", style = Theme.label.copy(fontSize = 10.sp))
    if (players.isEmpty()) RhText("Waiting for players…", style = Theme.small)
    players.forEach { p ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            androidx.compose.foundation.layout.Box(Modifier.size(8.dp).androidxBg(Color(0xFF5CB85C)))
            Spacer(Modifier.size(8.dp))
            RhText(p.name, Modifier.weight(1f))
            if (p.isDm) Tag("DM", Theme.gold)
            if (p.rttMs >= 0) RhText("${p.rttMs} ms", style = Theme.small)
            RhIconButton(RhIcon.CLOSE, { host.kick(p.deviceId) }, tint = Theme.onDarkSoft, size = 16.dp)
        }
    }
}

private fun Modifier.androidxBg(c: Color) = this.then(Modifier.background(c, androidx.compose.foundation.shape.CircleShape))

@Composable
fun CodesRow(code: String, dmCode: String, address: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { RhText("PLAYER CODE", style = Theme.label.copy(fontSize = 10.sp)); RhText(code, style = Theme.title.copy(fontSize = 30.sp, letterSpacing = 5.sp), color = Theme.gold) }
        Column(horizontalAlignment = Alignment.CenterHorizontally) { RhText("DM CODE", style = Theme.label.copy(fontSize = 10.sp)); RhText(dmCode, style = Theme.title.copy(fontSize = 30.sp, letterSpacing = 5.sp), color = Color(0xFF8CCB5A)) }
    }
    Spacer(Modifier.height(4.dp))
    RhText("Address $address", Modifier.fillMaxWidth(), style = Theme.small, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
}
