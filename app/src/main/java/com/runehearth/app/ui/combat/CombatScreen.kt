package com.runehearth.app.ui.combat

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runehearth.app.art.ArtCategory
import com.runehearth.app.art.AssetRef
import com.runehearth.app.data.Combatant
import com.runehearth.app.data.Encounter
import com.runehearth.app.engine.Action
import com.runehearth.app.session.newActionId
import com.runehearth.app.session.newEntityId
import com.runehearth.app.ui.assets.ArtField
import com.runehearth.app.ui.components.ArtSlot
import com.runehearth.app.ui.components.BtnKind
import com.runehearth.app.ui.components.Chip
import com.runehearth.app.ui.components.EmptyState
import com.runehearth.app.ui.components.Panel
import com.runehearth.app.ui.components.RhDialog
import com.runehearth.app.ui.components.RhIcon
import com.runehearth.app.ui.components.RhIconButton
import com.runehearth.app.ui.components.RhNumberField
import com.runehearth.app.ui.components.RhText
import com.runehearth.app.ui.components.RhTextField
import com.runehearth.app.ui.components.SectionHeader
import com.runehearth.app.ui.components.StatBar
import com.runehearth.app.ui.components.Tag
import com.runehearth.app.ui.components.WoodButton
import com.runehearth.app.ui.rememberDao
import com.runehearth.app.ui.rememberDispatch
import com.runehearth.app.ui.rememberFlow
import com.runehearth.app.ui.sheet.IntField
import com.runehearth.app.ui.sheet.StatusEditor
import com.runehearth.app.ui.sheet.StatusRow
import com.runehearth.app.ui.theme.Theme
import java.text.DateFormat
import java.util.Date

/** Read-only initiative tracker for players. */
@Composable
fun PlayerCombat(campaignId: String, characterId: String?, modifier: Modifier = Modifier) {
    val dao = rememberDao()
    val encounters by rememberFlow(campaignId, emptyList()) { dao.observeEncounters(campaignId) }
    val active = encounters.firstOrNull { it.active }
    Column(modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (active == null) EmptyState(RhIcon.SWORDS, "No battle underway", "When the Dungeon Master starts an encounter, the turn order appears here.")
        else EncounterBoard(active, dm = false, myCharacterId = characterId)
    }
}

/** Dungeon Master combat management: encounters, initiative, damage and status effects. */
@Composable
fun CombatScreen(campaignId: String, modifier: Modifier = Modifier) {
    val dao = rememberDao()
    val dispatch = rememberDispatch()
    val encounters by rememberFlow(campaignId, emptyList()) { dao.observeEncounters(campaignId) }
    val events by rememberFlow(campaignId, emptyList()) { dao.observeEvents(campaignId, 80) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }
    val selected = encounters.firstOrNull { it.id == selectedId } ?: encounters.firstOrNull { it.active } ?: encounters.firstOrNull()
    LazyColumn(modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(encounters, key = { it.id }) { e -> Chip(e.name + if (e.active) " ●" else "", selected?.id == e.id, { selectedId = e.id }) } }
                WoodButton("New", { creating = true }, icon = RhIcon.PLUS, compact = true, kind = BtnKind.GOLD)
            }
        }
        if (selected == null) item { EmptyState(RhIcon.SWORDS, "No encounters", "Create an encounter, add your party and enemies, roll initiative and begin.") }
        else {
            item { EncounterBoard(selected, dm = true, myCharacterId = null) }
        }
        item { SectionHeader("Combat Log") }
        val log = events.filter { it.type == "combat" || it.type == "vitals" || it.type == "cast" }
        if (log.isEmpty()) item { RhText("Nothing yet.", style = Theme.small, color = Theme.onDarkSoft) }
        items(log.take(30), key = { it.seq }) { e ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                RhText(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(e.ts)), style = Theme.small, modifier = Modifier.padding(end = 8.dp))
                RhText(e.text, style = Theme.small, color = Theme.onDark)
            }
        }
    }
    if (creating) {
        var name by remember { mutableStateOf("") }
        RhDialog({ creating = false }, "New Encounter", actions = {
            WoodButton("Cancel", { creating = false }, kind = BtnKind.SECONDARY, compact = true)
            WoodButton("Create", {
                val id = newEntityId(); selectedId = id
                dispatch(Action.UpsertEncounter(newActionId(), Encounter(id, campaignId, name))); creating = false
            }, kind = BtnKind.GOLD, compact = true, enabled = name.isNotBlank())
        }) { RhTextField(name, { name = it.take(60) }, label = "Encounter name", onDark = false) }
    }
}

@Composable
private fun EncounterBoard(e: Encounter, dm: Boolean, myCharacterId: String?) {
    val dao = rememberDao()
    val dispatch = rememberDispatch()
    val list by rememberFlow(e.id, emptyList()) { dao.observeCombatants(e.id) }
    var addingEnemy by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Panel(Modifier.fillMaxWidth(), surface = com.runehearth.app.ui.components.Surface.STONE) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    RhText(e.name, style = Theme.heading, maxLines = 1)
                    RhText(if (e.active) "Round ${e.round} · ${list.size} combatants" else "Not started · ${list.size} combatants", style = Theme.small)
                }
                if (e.active) Tag("In battle", Color(0xFFD9453B))
            }
            if (dm) {
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { WoodButton("Add party", { dispatch(Action.AddPartyToEncounter(newActionId(), e.id)) }, compact = true, kind = BtnKind.SECONDARY, icon = RhIcon.USER) }
                    item { WoodButton("Add enemy", { addingEnemy = true }, compact = true, kind = BtnKind.SECONDARY, icon = RhIcon.SKULL) }
                    item { WoodButton("Roll initiative", { dispatch(Action.RollInitiative(newActionId(), e.id)) }, compact = true, kind = BtnKind.SECONDARY, icon = RhIcon.DICE) }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!e.active) WoodButton("Start", { dispatch(Action.StartEncounter(newActionId(), e.id)) }, Modifier.weight(1f), kind = BtnKind.GOLD, icon = RhIcon.SWORDS, compact = true, enabled = list.isNotEmpty())
                    else {
                        WoodButton("Next turn", { dispatch(Action.NextTurn(newActionId(), e.id)) }, Modifier.weight(1f), kind = BtnKind.GOLD, icon = RhIcon.ARROW, compact = true)
                        WoodButton("End", { dispatch(Action.EndEncounter(newActionId(), e.id)) }, kind = BtnKind.DANGER, compact = true)
                    }
                    WoodButton("", { deleting = true }, kind = BtnKind.SECONDARY, icon = RhIcon.TRASH, compact = true)
                }
            }
        }
        if (list.isEmpty()) EmptyState(RhIcon.SKULL, "Empty battlefield", if (dm) "Add your party and some enemies." else "Waiting for combatants…")
        list.forEachIndexed { idx, c -> CombatantCard(c, current = e.active && idx == e.turnIndex, dm = dm, mine = c.characterId != null && c.characterId == myCharacterId) }
    }
    if (addingEnemy) EnemyDialog(e, { addingEnemy = false })
    if (deleting) com.runehearth.app.ui.components.ConfirmDialog("Delete encounter?", "${e.name} and its combatants will be removed.", "Delete", { dispatch(Action.DeleteEncounter(newActionId(), e.id)) }, { deleting = false })
}

@Composable
private fun CombatantCard(c: Combatant, current: Boolean, dm: Boolean, mine: Boolean) {
    val dispatch = rememberDispatch()
    val dao = rememberDao()
    val statuses by rememberFlow(c.id, emptyList()) { dao.observeStatuses(c.id) }
    var delta by remember { mutableLongStateOf(0) }
    var adding by remember { mutableStateOf(false) }
    val glow by animateColorAsState(if (current) Color(0xFFFFD54A) else Theme.goldDark, label = "turn")
    Panel(Modifier.fillMaxWidth(), padding = 8.dp, glow = glow) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            ArtSlot(AssetRef.decode(c.art), 52.dp, if (c.isPlayer) Color(0xFF5CB85C) else Color(0xFFD9453B))
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    RhText(c.name, Modifier.weight(1f, fill = false), style = Theme.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), maxLines = 1)
                    if (current) Tag("Turn", Color(0xFFFFD54A))
                    if (mine) Tag("You", Color(0xFF5CB85C))
                }
                StatBar("HP", c.hp, c.maxHp, if (c.hp * 4 <= c.maxHp) Color(0xFFD9453B) else Color(0xFF5CB85C))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) { RhText("INIT", style = Theme.label.copy(fontSize = 9.sp)); RhText(c.initiative.toString(), style = Theme.heading) }
        }
        if (statuses.isNotEmpty()) { Spacer(Modifier.height(4.dp)); statuses.forEach { s -> StatusRow(s, dm) { dispatch(Action.DeleteStatus(newActionId(), s.id)) } } }
        if (dm) {
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                items(listOf(-10, -5, -1, 1, 5, 10)) { d -> Chip(if (d > 0) "+$d" else "$d", false, { dispatch(Action.DamageCombatant(newActionId(), c.id, d)) }, color = if (d < 0) Color(0xFFD9453B) else Color(0xFF5CB85C)) }
                item { Chip("Status", false, { adding = true }, color = Theme.steel) }
                item { Chip("Remove", false, { dispatch(Action.DeleteCombatant(newActionId(), c.id)) }, color = Theme.steel) }
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                RhNumberField(delta, { delta = it }, Modifier.weight(1f), "Custom amount")
                WoodButton("Damage", { dispatch(Action.DamageCombatant(newActionId(), c.id, -delta.toInt())); delta = 0 }, compact = true, kind = BtnKind.DANGER, enabled = delta > 0)
                WoodButton("Heal", { dispatch(Action.DamageCombatant(newActionId(), c.id, delta.toInt())); delta = 0 }, compact = true, enabled = delta > 0)
            }
        }
    }
    if (adding) StatusEditor(c.id) { adding = false }
}

@Composable
private fun EnemyDialog(e: Encounter, onDismiss: () -> Unit) {
    val dispatch = rememberDispatch()
    var name by remember { mutableStateOf("") }
    var hp by remember { mutableStateOf(20) }
    var def by remember { mutableStateOf(2) }
    var init by remember { mutableStateOf(0) }
    var count by remember { mutableStateOf(1) }
    var art by remember { mutableStateOf(AssetRef(ArtCategory.MONSTER, 1, 6, false).encode()) }
    RhDialog(onDismiss, "Add Enemy", modifier = Modifier.height(520.dp), actions = {
        WoodButton("Cancel", onDismiss, kind = BtnKind.SECONDARY, compact = true)
        WoodButton("Add", {
            repeat(count) { n -> dispatch(Action.UpsertCombatant(newActionId(), Combatant(newEntityId(), e.id, e.campaignId, null, if (count > 1) "$name ${n + 1}" else name, init, hp, hp, def, false, art))) }
            onDismiss()
        }, kind = BtnKind.GOLD, compact = true, enabled = name.isNotBlank())
    }) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ArtField("Monster image", art, listOf(ArtCategory.MONSTER), { art = it })
            RhTextField(name, { name = it.take(40) }, label = "Name", onDark = false)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { IntField("HP", hp, { hp = it.coerceAtLeast(1) }, Modifier.weight(1f)); IntField("Defense", def, { def = it }, Modifier.weight(1f)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { IntField("Initiative", init, { init = it }, Modifier.weight(1f)); IntField("How many", count, { count = it.coerceIn(1, 20) }, Modifier.weight(1f)) }
        }
    }
}
