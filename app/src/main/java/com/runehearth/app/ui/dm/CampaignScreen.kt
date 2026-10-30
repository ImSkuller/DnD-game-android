package com.runehearth.app.ui.dm

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
import com.runehearth.app.core.Money
import com.runehearth.app.data.Entry
import com.runehearth.app.data.EntryStatus
import com.runehearth.app.data.EntryType
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
import com.runehearth.app.ui.components.RhText
import com.runehearth.app.ui.components.RhTextField
import com.runehearth.app.ui.components.Surface
import com.runehearth.app.ui.components.Tag
import com.runehearth.app.ui.components.WoodButton
import com.runehearth.app.ui.economy.CoinInput
import com.runehearth.app.ui.rememberDao
import com.runehearth.app.ui.rememberDispatch
import com.runehearth.app.ui.rememberFlow
import com.runehearth.app.ui.theme.Theme

/** Quests, NPCs, locations, lore, events, notes, rewards and achievements. */
@Composable
fun CampaignScreen(campaignId: String, modifier: Modifier = Modifier) {
    val dao = rememberDao()
    val dispatch = rememberDispatch()
    val entries by rememberFlow(campaignId, emptyList()) { dao.observeAllEntries(campaignId) }
    var type by rememberSaveable { mutableStateOf(EntryType.QUEST.name) }
    var editing by remember { mutableStateOf<Entry?>(null) }
    var creating by remember { mutableStateOf(false) }
    val t = EntryType.valueOf(type)
    val list = entries.filter { it.type == t }
    Column(modifier.fillMaxSize()) {
        LazyRow(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)) {
            items(EntryType.entries) { e -> Chip(e.label + "s".takeIf { e != EntryType.LORE } .orEmpty(), t == e, { type = e.name }) }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { WoodButton("New ${t.label.lowercase()}", { creating = true }, Modifier.fillMaxWidth(), icon = RhIcon.PLUS, kind = BtnKind.GOLD, compact = true) }
            if (list.isEmpty()) item { EmptyState(RhIcon.SCROLL, "Nothing recorded", "Keep track of your ${t.label.lowercase()}s here.") }
            items(list, key = { it.id }) { e ->
                Panel(Modifier.fillMaxWidth(), surface = Surface.PARCHMENT, onClick = { editing = e }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (e.art.isNotBlank()) ArtSlot(com.runehearth.app.art.AssetRef.decode(e.art), 48.dp)
                        Column(Modifier.weight(1f)) {
                            RhText(e.title, style = Theme.heading.copy(fontSize = 17.sp), maxLines = 1)
                            if (e.body.isNotBlank()) RhText(e.body, style = Theme.small, maxLines = 3)
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            if (t == EntryType.QUEST) Tag(e.status.label, when (e.status) { EntryStatus.ACTIVE -> Color(0xFFB7791F); EntryStatus.COMPLETED -> Color(0xFF2E7D32); else -> Color(0xFFB3261E) })
                            Tag(if (e.ownerId != null) "Assigned" else if (e.isPublic) "Shared" else "DM only", if (e.isPublic || e.ownerId != null) Color(0xFF2A5FA8) else Color(0xFF6B5B4A))
                        }
                    }
                    if (t == EntryType.QUEST && e.status == EntryStatus.ACTIVE) {
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            WoodButton("Complete", { dispatch(Action.CompleteQuest(newActionId(), e.id, true)) }, compact = true)
                            WoodButton("Fail", { dispatch(Action.CompleteQuest(newActionId(), e.id, false)) }, compact = true, kind = BtnKind.DANGER)
                            if (e.rewardBronze > 0) RhText("Reward ${Money.format(e.rewardBronze)}", style = Theme.small, color = Theme.goldDark)
                        }
                    }
                }
            }
        }
    }
    if (creating) EntryEditor(campaignId, t, null) { creating = false }
    editing?.let { EntryEditor(campaignId, it.type, it) { editing = null } }
}

@Composable
fun EntryEditor(campaignId: String, type: EntryType, initial: Entry?, onDismiss: () -> Unit) {
    val dispatch = rememberDispatch()
    val dao = rememberDao()
    val chars by rememberFlow(campaignId, emptyList()) { dao.observeCharacters(campaignId) }
    var e by remember { mutableStateOf(initial ?: Entry(newEntityId(), campaignId, type, "")) }
    var reward by remember { mutableLongStateOf(e.rewardBronze) }
    var confirmDelete by remember { mutableStateOf(false) }
    RhDialog(onDismiss, if (initial == null) "New ${type.label}" else "Edit ${type.label}", modifier = Modifier.height(600.dp), actions = {
        if (initial != null) WoodButton("Delete", { confirmDelete = true }, kind = BtnKind.DANGER, compact = true)
        WoodButton("Cancel", onDismiss, kind = BtnKind.SECONDARY, compact = true)
        WoodButton("Save", { dispatch(Action.UpsertEntry(newActionId(), e.copy(rewardBronze = reward))); onDismiss() }, kind = BtnKind.GOLD, compact = true, enabled = e.title.isNotBlank())
    }) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RhTextField(e.title, { e = e.copy(title = it.take(100)) }, label = "Title", onDark = false)
            RhTextField(e.body, { e = e.copy(body = it) }, label = "Details", singleLine = false, minLines = 4, onDark = false)
            if (type == EntryType.NPC || type == EntryType.LOCATION || type == EntryType.REWARD || type == EntryType.ACHIEVEMENT)
                ArtField("Image", e.art, if (type == EntryType.NPC) listOf(ArtCategory.MONSTER, ArtCategory.HELMET) else ArtCategory.entries, { e = e.copy(art = it) })
            if (type == EntryType.QUEST) {
                RhText("STATUS", style = Theme.label.copy(fontSize = 10.5.sp), color = Theme.goldDark)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { EntryStatus.entries.forEach { s -> Chip(s.label, e.status == s, { e = e.copy(status = s) }) } }
                RhText("REWARD (paid to the party purse on completion)", style = Theme.label.copy(fontSize = 10.5.sp), color = Theme.goldDark)
                CoinInput({ reward = it })
            }
            RhText("VISIBILITY", style = Theme.label.copy(fontSize = 10.5.sp), color = Theme.goldDark)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item { Chip("DM only", e.ownerId == null && !e.isPublic, { e = e.copy(ownerId = null, isPublic = false) }) }
                item { Chip("All players", e.ownerId == null && e.isPublic, { e = e.copy(ownerId = null, isPublic = true) }) }
                items(chars.filter { !it.isNpc }) { c -> Chip(c.name, e.ownerId == c.id, { e = e.copy(ownerId = c.id) }) }
            }
            Spacer(Modifier.height(2.dp))
        }
    }
    if (confirmDelete) com.runehearth.app.ui.components.ConfirmDialog("Delete?", "\"${e.title}\" will be removed.", "Delete", { dispatch(Action.DeleteEntry(newActionId(), e.id)); onDismiss() }, { confirmDelete = false })
}
