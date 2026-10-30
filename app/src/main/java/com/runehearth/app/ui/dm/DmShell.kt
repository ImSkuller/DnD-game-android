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

private val DM_TABS = listOf(
    TabSpec("Hall", RhIcon.CASTLE), TabSpec("Party", RhIcon.SHIELD), TabSpec("Purse", RhIcon.COIN),
    TabSpec("Dice", RhIcon.DICE), TabSpec("Combat", RhIcon.SWORDS), TabSpec("Lore", RhIcon.SCROLL),
)

/** Dungeon Master interface: dashboard, party, economy, dice, combat and campaign tools. */
@Composable
fun DmShell(campaignId: String, onExit: () -> Unit) {
    val dao = rememberDao()
    val session = LocalSession.current
    val camp by rememberFlow(campaignId, null) { dao.observeCampaign(campaignId) }
    val conn by session.connection.collectAsState()
    var tab by rememberSaveable { mutableStateOf(0) }
    var charId by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = charId != null) { charId = null }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(
            title = camp?.name ?: "Campaign",
            onBack = if (charId != null) ({ charId = null }) else onExit,
            actions = {
                val label = when (conn) { ConnectionState.HOSTING -> "Hosting"; else -> if (camp?.mode == CampaignMode.COOP) "Co-op" else "Local" }
                Tag(label, if (conn == ConnectionState.HOSTING) Color(0xFF5CB85C) else Theme.steel)
                Spacer(Modifier.size(10.dp))
            },
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AnimatedContent(if (charId != null) -1 else tab, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) }, label = "dmTab") { t ->
                when (t) {
                    -1 -> CharacterSheet(charId!!, onDeleted = { charId = null })
                    0 -> Dashboard(campaignId, onOpenCharacter = { tab = 1; charId = it }, onTab = { tab = it })
                    1 -> PartyScreen(campaignId, onOpen = { charId = it })
                    2 -> EconomyScreen(campaignId)
                    3 -> DiceScreen(campaignId)
                    4 -> CombatScreen(campaignId)
                    else -> CampaignScreen(campaignId)
                }
            }
        }
        if (charId == null) RuneTabBar(DM_TABS, tab, { tab = it })
    }
}

@Composable
fun PartyScreen(campaignId: String, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    val dao = rememberDao()
    val chars by rememberFlow(campaignId, emptyList()) { dao.observeCharacters(campaignId) }
    var creating by remember { mutableStateOf(false) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { WoodButton("Add character", { creating = true }, Modifier.fillMaxWidth(), icon = RhIcon.PLUS, kind = BtnKind.GOLD, compact = true) }
        if (chars.isEmpty()) item { EmptyState(RhIcon.SHIELD, "No characters yet", "Create adventurers (or let players join a co-op session and create their own).") }
        items(chars, key = { it.id }) { c -> CharacterCard(c) { onOpen(c.id) } }
    }
    if (creating) CharacterEditor(campaignId, null) { creating = false }
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
private fun Dashboard(campaignId: String, onOpenCharacter: (String) -> Unit, onTab: (Int) -> Unit) {
    val dao = rememberDao()
    val session = LocalSession.current
    val app = LocalApp.current
    val toaster = LocalToaster.current
    val dispatch = rememberDispatch()
    val camp by rememberFlow(campaignId, null) { dao.observeCampaign(campaignId) }
    val chars by rememberFlow(campaignId, emptyList()) { dao.observeCharacters(campaignId) }
    val events by rememberFlow(campaignId, emptyList()) { dao.observeEvents(campaignId, 12) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val host = session as? HostSession
    var tools by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (host != null) item { SessionPanel(host) }
        else item {
            Panel(Modifier.fillMaxWidth()) {
                RhText("Local Only campaign", style = Theme.heading)
                RhText("You manage everyone on this device. Balances are visible only to you.", style = Theme.small)
            }
        }
        item {
            Panel(Modifier.fillMaxWidth(), surface = Surface.STONE) {
                SectionHeader("Quick Actions")
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { WoodButton("Roll d20", { onTab(3) }, icon = RhIcon.DICE, compact = true) }
                    item { WoodButton("Reward gold", { onTab(2) }, icon = RhIcon.COIN, compact = true, kind = BtnKind.SECONDARY) }
                    item { WoodButton("Start battle", { onTab(4) }, icon = RhIcon.SWORDS, compact = true, kind = BtnKind.SECONDARY) }
                    item { WoodButton("Tools", { tools = true }, icon = RhIcon.GEM, compact = true, kind = BtnKind.SECONDARY) }
                }
            }
        }
        item { SectionHeader("The Party") }
        if (chars.none { !it.isNpc }) item { EmptyState(RhIcon.SHIELD, "No adventurers yet", "Open the Party tab to create characters.") }
        items(chars.filter { !it.isNpc }, key = { it.id }) { c -> CharacterCard(c) { onOpenCharacter(c.id) } }
        item { SectionHeader("Recent Events") }
        if (events.isEmpty()) item { RhText("The chronicle is empty.", style = Theme.small, color = Theme.onDarkSoft) }
        items(events, key = { it.seq }) { e -> RhText("• ${e.text}", style = Theme.small, color = Theme.onDark) }
    }
    if (tools) camp?.let { DmTools(it, { tools = false }) }
}

@Composable
private fun SessionPanel(host: HostSession) {
    val hosting by host.hosting.collectAsState()
    val code by host.code.collectAsState()
    val address by host.address.collectAsState()
    val players by host.players.collectAsState()
    val toaster = LocalToaster.current
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) { RhText("PAIRING CODE", style = Theme.label.copy(fontSize = 10.sp)); RhText(code, style = Theme.title.copy(fontSize = 34.sp, letterSpacing = 6.sp), color = Theme.gold) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) { RhText("ADDRESS", style = Theme.label.copy(fontSize = 10.sp)); RhText(address, style = Theme.heading.copy(fontSize = 17.sp)) }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WoodButton("New code", { host.regenerateCode() }, compact = true, kind = BtnKind.SECONDARY)
                WoodButton("Close session", { host.stopHosting() }, compact = true, kind = BtnKind.DANGER)
            }
            Spacer(Modifier.height(8.dp))
            RhText("CONNECTED (${players.size})", style = Theme.label.copy(fontSize = 10.sp))
            if (players.isEmpty()) RhText("Waiting for players…", style = Theme.small)
            players.forEach { p ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.size(8.dp).androidxBg(Color(0xFF5CB85C)))
                    Spacer(Modifier.size(8.dp))
                    RhText(p.name, Modifier.weight(1f))
                    RhIconButton(RhIcon.CLOSE, { host.kick(p.deviceId) }, tint = Theme.onDarkSoft, size = 16.dp)
                }
            }
        } else {
            WoodButton("Open session", { if (!host.startHosting()) toaster.show("Could not open the network port", true) }, Modifier.fillMaxWidth(), icon = RhIcon.WIFI, kind = BtnKind.GOLD, compact = true)
        }
    }
}

@Composable
private fun RhIconView2(on: Boolean) = com.runehearth.app.ui.components.RhIconView(RhIcon.WIFI, if (on) Color(0xFF5CB85C) else Theme.onDarkSoft, 30.dp)

private fun Modifier.androidxBg(c: Color) = this.then(Modifier.background(c, androidx.compose.foundation.shape.CircleShape))
