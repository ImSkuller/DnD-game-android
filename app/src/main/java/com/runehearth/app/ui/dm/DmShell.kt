package com.runehearth.app.ui.dm

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runehearth.app.art.AssetRef
import com.runehearth.app.data.CampaignMode
import com.runehearth.app.data.GameCharacter
import com.runehearth.app.engine.Action
import com.runehearth.app.net.HostSession
import com.runehearth.app.session.ConnectionState
import com.runehearth.app.session.newActionId
import com.runehearth.app.ui.LocalApp
import com.runehearth.app.ui.LocalSession
import com.runehearth.app.ui.combat.CombatScreen
import com.runehearth.app.ui.components.ArtSlot
import com.runehearth.app.ui.components.BtnKind
import com.runehearth.app.ui.components.EmptyState
import com.runehearth.app.ui.components.LocalToaster
import com.runehearth.app.ui.components.Panel
import com.runehearth.app.ui.components.RhIcon
import com.runehearth.app.ui.components.RhIconButton
import com.runehearth.app.ui.components.RhText
import com.runehearth.app.ui.components.RuneTabBar
import com.runehearth.app.ui.components.ScreenTopBar
import com.runehearth.app.ui.components.SectionHeader
import com.runehearth.app.ui.components.StatBar
import com.runehearth.app.ui.components.Surface
import com.runehearth.app.ui.components.TabSpec
import com.runehearth.app.ui.components.Tag
import com.runehearth.app.ui.components.WoodButton
import com.runehearth.app.ui.dice.DiceScreen
import com.runehearth.app.ui.economy.EconomyScreen
import com.runehearth.app.ui.rememberDao
import com.runehearth.app.ui.rememberDispatch
import com.runehearth.app.ui.rememberFlow
import com.runehearth.app.ui.sheet.CharacterEditor
import com.runehearth.app.ui.sheet.CharacterSheet
import com.runehearth.app.ui.theme.Theme

private val DM_TABS = listOf(TabSpec("Party", RhIcon.SHIELD), TabSpec("Purse", RhIcon.COIN), TabSpec("Dice", RhIcon.DICE), TabSpec("Battle", RhIcon.SWORDS))

/** Dungeon Master interface: dashboard, party, economy, dice, combat and campaign tools. */
@Composable
fun DmShell(campaignId: String, onExit: () -> Unit) {
    val dao = rememberDao()
    val session = LocalSession.current
    val camp by rememberFlow(campaignId, null) { dao.observeCampaign(campaignId) }
    val conn by session.connection.collectAsState()
    var tab by rememberSaveable { mutableStateOf(0) }
    var charId by rememberSaveable { mutableStateOf<String?>(null) }
    var story by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = charId != null || story) { if (charId != null) charId = null else story = false }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(
            title = camp?.name ?: "Campaign",
            onBack = if (charId != null) ({ charId = null }) else if (story) ({ story = false }) else onExit,
            actions = {
                NotificationBell(campaignId)
                val label = when (conn) { ConnectionState.HOSTING -> "Hosting"; else -> if (camp?.mode == CampaignMode.COOP) "Co-op" else "Local" }
                Tag(label, if (conn == ConnectionState.HOSTING) Color(0xFF5CB85C) else Theme.steel)
                Spacer(Modifier.size(10.dp))
            },
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AnimatedContent(Triple(charId, story, tab), transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) }, label = "dmTab") { (cid, st, t) ->
                when {
                    cid != null -> CharacterSheet(cid, onDeleted = { charId = null })
                    st -> CampaignScreen(campaignId)
                    t == 0 -> PartyHome(campaignId, onOpenCharacter = { charId = it }, onStory = { story = true })
                    t == 1 -> EconomyScreen(campaignId)
                    t == 2 -> DiceScreen(campaignId)
                    else -> CombatScreen(campaignId)
                }
            }
        }
        if (charId == null) RuneTabBar(DM_TABS, tab, { tab = it; story = false })
    }
}

@Composable
fun CharacterCard(c: GameCharacter, onClick: () -> Unit) {
    val dao = rememberDao()
    val equipped by rememberFlow(c.id, emptyList()) { dao.observeEquippedOf(c.id) }
    val statuses by rememberFlow(c.id, emptyList()) { dao.observeStatuses(c.id) }
    val stats = remember(c, equipped, statuses) { com.runehearth.app.engine.StatCalculator.compute(c, equipped, statuses) }
    Panel(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ArtSlot(AssetRef.decode(c.art), 64.dp, Theme.gold)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    RhText(c.name, Modifier.weight(1f, fill = false), style = Theme.heading, maxLines = 1)
                    if (c.isNpc) Tag("NPC", Theme.steel)
                }
                RhText("Lv ${c.level} ${c.race} ${c.className}", style = Theme.small)
                StatBar("HP", c.curHp, stats.maxHp, Color(0xFFD9453B), showNumbers = true)
                StatBar("MP", c.curMp, stats.maxMp, Color(0xFF4F8DE0), showNumbers = true)
            }
        }
    }
}

@Composable
private fun PartyHome(campaignId: String, onOpenCharacter: (String) -> Unit, onStory: () -> Unit) {
    val dao = rememberDao()
    val session = LocalSession.current
    val camp by rememberFlow(campaignId, null) { dao.observeCampaign(campaignId) }
    val chars by rememberFlow(campaignId, emptyList()) { dao.observeCharacters(campaignId) }
    val events by rememberFlow(campaignId, emptyList()) { dao.observeEvents(campaignId, 12) }
    val host = session as? HostSession
    var tools by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (host != null) item { SessionPanel(host) }
        else if (session is com.runehearth.app.net.RemoteSession) item { ControllerPanel(session) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WoodButton("Add hero", { creating = true }, Modifier.weight(1f), icon = RhIcon.PLUS, kind = BtnKind.GOLD, compact = true)
                WoodButton("Story", onStory, Modifier.weight(1f), icon = RhIcon.SCROLL, kind = BtnKind.SECONDARY, compact = true)
                WoodButton("Tools", { tools = true }, Modifier.weight(1f), icon = RhIcon.GEM, kind = BtnKind.SECONDARY, compact = true)
            }
        }
        if (chars.isEmpty()) item { EmptyState(RhIcon.SHIELD, "No heroes yet", "Tap Add hero to create your first adventurer.") }
        items(chars.filter { !it.isNpc }, key = { it.id }) { c -> CharacterCard(c) { onOpenCharacter(c.id) } }
        if (chars.any { it.isNpc }) item {
            com.runehearth.app.ui.components.Collapsible("NPCs") {
                chars.filter { it.isNpc }.forEach { c -> CharacterCard(c) { onOpenCharacter(c.id) }; Spacer(Modifier.height(8.dp)) }
            }
        }
        if (events.isNotEmpty()) item {
            com.runehearth.app.ui.components.Collapsible("Recent events") { events.forEach { e -> RhText("• ${e.text}", style = Theme.small, color = Theme.onDark) } }
        }
    }
    if (tools) camp?.let { DmTools(it, { tools = false }) }
    if (creating) CharacterEditor(campaignId, null) { creating = false }
}

@Composable
private fun SessionPanel(host: HostSession) {
    val hosting by host.hosting.collectAsState()
    val code by host.code.collectAsState()
    val dmCode by host.dmCode.collectAsState()
    val address by host.address.collectAsState()
    val players by host.players.collectAsState()
    val metrics by host.metrics.collectAsState()
    val toaster = LocalToaster.current
    var showHealth by remember { mutableStateOf(false) }
    Panel(Modifier.fillMaxWidth(), glow = if (hosting) Color(0xFF5CB85C) else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RhIconView2(hosting)
            Spacer(Modifier.size(8.dp))
            Column(Modifier.weight(1f)) {
                RhText(if (hosting) "Session open" else "Co-op session closed", style = Theme.heading)
                RhText(if (hosting) "Players join from the Join Game screen — same Wi-Fi or your phone's hotspot." else "Open the session so players can join.", style = Theme.small)
            }
        }
        Spacer(Modifier.height(8.dp))
        if (hosting) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                RhText("PLAYERS ENTER THIS CODE", style = Theme.label.copy(fontSize = 10.sp))
                RhText(code, style = Theme.title.copy(fontSize = 38.sp, letterSpacing = 6.sp), color = Theme.gold)
            }
            Spacer(Modifier.height(6.dp))
            ConnectedList(players, host)
            Spacer(Modifier.height(4.dp))
            com.runehearth.app.ui.components.Collapsible("Advanced") {
                CodesRow(code, dmCode, address)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WoodButton("New code", { host.regenerateCode() }, compact = true, kind = BtnKind.SECONDARY)
                    WoodButton("Health", { showHealth = !showHealth }, compact = true, kind = BtnKind.SECONDARY)
                }
                if (showHealth) { Spacer(Modifier.height(8.dp)); HostMetricsView(metrics) }
            }
            WoodButton("Close session", { host.stopHosting() }, Modifier.fillMaxWidth(), compact = true, kind = BtnKind.DANGER)
        } else {
            WoodButton("Open session", { if (!host.startHosting()) toaster.show("Could not open the network port", true) }, Modifier.fillMaxWidth(), icon = RhIcon.WIFI, kind = BtnKind.GOLD, compact = true)
        }
    }
}

@Composable
private fun ControllerPanel(remote: com.runehearth.app.net.RemoteSession) {
    val state by remote.connection.collectAsState()
    val latency by remote.latencyMs.collectAsState()
    Panel(Modifier.fillMaxWidth(), glow = if (state == ConnectionState.CONNECTED) Color(0xFF8CCB5A) else Color(0xFFE48A3C)) {
        RhText("Controlling a remote host", style = Theme.heading)
        RhText(
            when (state) {
                ConnectionState.CONNECTED -> "Connected${if (latency >= 0) " · $latency ms" else ""}. The host device validates and stores everything you do."
                else -> "Reconnecting to the host… Your view is saved on this device and refreshes when the link returns."
            },
            style = Theme.small,
        )
    }
}

@Composable
private fun RhIconView2(on: Boolean) = com.runehearth.app.ui.components.RhIconView(RhIcon.WIFI, if (on) Color(0xFF5CB85C) else Theme.onDarkSoft, 30.dp)

private fun Modifier.androidxBg(c: Color) = this.then(Modifier.background(c, androidx.compose.foundation.shape.CircleShape))
