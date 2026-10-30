package com.runehearth.app.ui.sheet

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runehearth.app.art.AssetRef
import com.runehearth.app.core.Money
import com.runehearth.app.data.EntryStatus
import com.runehearth.app.data.EntryType
import com.runehearth.app.data.EquipSlot
import com.runehearth.app.data.GameCharacter
import com.runehearth.app.data.Item
import com.runehearth.app.data.ItemKind
import com.runehearth.app.data.Skill
import com.runehearth.app.data.StatusEffect
import com.runehearth.app.data.Wallet
import com.runehearth.app.engine.Action
import com.runehearth.app.engine.Leveling
import com.runehearth.app.engine.StatCalculator
import com.runehearth.app.session.newActionId
import com.runehearth.app.ui.LocalSession
import com.runehearth.app.ui.components.BtnKind
import com.runehearth.app.ui.components.ArtSlot
import com.runehearth.app.ui.components.Chip
import com.runehearth.app.ui.components.CoinRow
import com.runehearth.app.ui.components.ConfirmDialog
import com.runehearth.app.ui.components.EmptyState
import com.runehearth.app.ui.components.KeyValue
import com.runehearth.app.ui.components.LocalToaster
import com.runehearth.app.ui.components.Panel
import com.runehearth.app.ui.components.RhDialog
import com.runehearth.app.ui.components.RhIcon
import com.runehearth.app.ui.components.RhIconButton
import com.runehearth.app.ui.components.RhIconView
import com.runehearth.app.ui.components.RhNumberField
import com.runehearth.app.ui.components.RhText
import com.runehearth.app.ui.components.RhTextField
import com.runehearth.app.ui.components.SectionHeader
import com.runehearth.app.ui.components.StatBar
import com.runehearth.app.ui.components.Stepper
import com.runehearth.app.ui.components.Surface
import com.runehearth.app.ui.components.Tag
import com.runehearth.app.ui.components.WoodButton
import com.runehearth.app.ui.components.pressable
import com.runehearth.app.ui.rememberDao
import com.runehearth.app.ui.rememberDispatch
import com.runehearth.app.ui.rememberFlow
import com.runehearth.app.ui.theme.Theme

private val TABS = listOf("Overview", "Skills", "Gear", "Pack", "Effects", "Journal")

/** The full character sheet. The DM gets every control; a player only the controls the host will accept. */
@Composable
fun CharacterSheet(characterId: String, modifier: Modifier = Modifier, onDeleted: () -> Unit = {}) {
    val session = LocalSession.current
    val dm = session.isDm
    val dao = rememberDao()
    val ch by rememberFlow(characterId, null) { dao.observeCharacter(characterId) }
    val equipped by rememberFlow(characterId, emptyList()) { dao.observeEquippedOf(characterId) }
    val statuses by rememberFlow(characterId, emptyList()) { dao.observeStatuses(characterId) }
    var tab by rememberSaveable { mutableStateOf(0) }
    val c = ch
    if (c == null) { EmptyState(RhIcon.USER, "Character not found", "This character is not available on this device yet.", modifier); return }
    val stats = remember(c, equipped, statuses) { StatCalculator.compute(c, equipped, statuses) }

    Column(modifier.fillMaxSize()) {
        LazyRow(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)) {
            items(TABS.size) { i -> Chip(TABS[i], tab == i, { tab = i }) }
        }
        AnimatedContent(tab, Modifier.weight(1f), transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) }, label = "sheetTab") { t ->
            when (t) {
                0 -> OverviewTab(c, stats, dm, onDeleted)
                1 -> SkillsTab(c, dm)
                2 -> GearTab(c, equipped, dm)
                3 -> PackTab(c, equipped, dm)
                4 -> EffectsTab(c, statuses, dm)
                else -> JournalTab(c, dm)
            }
        }
    }
}

@Composable
private fun Section(content: @Composable () -> Unit) = Box(Modifier.padding(horizontal = 12.dp)) { content() }

@Composable
private fun OverviewTab(c: GameCharacter, stats: com.runehearth.app.engine.FinalStats, dm: Boolean, onDeleted: () -> Unit) {
    val dao = rememberDao()
    val dispatch = rememberDispatch()
    val wallet by rememberFlow(c.id, null) { dao.observeWallet(Wallet.personalId(c.id)) }
    var editing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var xp by remember { mutableStateOf(0L) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Panel(Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    ArtSlot(AssetRef.decode(c.art), 88.dp, Theme.gold)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        RhText(c.name, style = Theme.title.copy(fontSize = 22.sp, letterSpacing = 1.sp), maxLines = 2)
                        RhText("Level ${c.level} ${c.race} ${c.className}", style = Theme.body, color = Theme.gold)
                        if (c.job.isNotBlank()) RhText(c.job, style = Theme.small)
                        if (c.isNpc) Tag("NPC", Theme.steel)
                    }
                    if (dm) RhIconButton(RhIcon.EDIT, { editing = true }, size = 20.dp)
                }
                Spacer(Modifier.height(10.dp))
                val need = Leveling.expToNext(c.level)
                StatBar("Experience", c.exp.toInt(), need.toInt(), androidx.compose.ui.graphics.Color(0xFF6C8CD5))
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                StatBar("Health", c.curHp, stats.maxHp, androidx.compose.ui.graphics.Color(0xFFD9453B))
                Spacer(Modifier.height(8.dp))
                StatBar("Mana", c.curMp, stats.maxMp, androidx.compose.ui.graphics.Color(0xFF4F8DE0))
                if (dm) {
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf(-10 to 0, -5 to 0, -1 to 0, 1 to 0, 5 to 0, 10 to 0)) { (h, _) -> Chip("${if (h > 0) "+" else ""}$h HP", false, { dispatch(Action.AdjustVitals(newActionId(), c.id, h, 0)) }, color = if (h < 0) androidx.compose.ui.graphics.Color(0xFFD9453B) else androidx.compose.ui.graphics.Color(0xFF5CB85C)) }
                        items(listOf(-5, 5, 10)) { m -> Chip("${if (m > 0) "+" else ""}$m MP", false, { dispatch(Action.AdjustVitals(newActionId(), c.id, 0, m)) }, color = androidx.compose.ui.graphics.Color(0xFF4F8DE0)) }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                        RhNumberField(xp, { xp = it }, Modifier.weight(1f), "Award experience", max = 1_000_000)
                        WoodButton("Award XP", { dispatch(Action.AwardExp(newActionId(), c.id, xp)); xp = 0 }, compact = true, enabled = xp > 0)
                    }
                }
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                SectionHeader("Combat Stats")
                StatRow("Attack", stats.atk); StatRow("Defense", stats.def); StatRow("Speed", stats.spd)
                StatRow("Max HP", stats.hp); StatRow("Max MP", stats.mp)
                Spacer(Modifier.height(6.dp))
                RhText("Final = base + equipment + active effects", style = Theme.small)
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                SectionHeader("Attributes")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    for ((n, v) in listOf("STR" to c.str, "DEX" to c.dex, "CON" to c.con, "INT" to c.intel, "WIS" to c.wis, "CHA" to c.cha)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { RhText(n, style = Theme.label.copy(fontSize = 10.sp)); RhText(v.toString(), style = Theme.heading) }
                    }
                }
            }
        }
        wallet?.let { w ->
            item {
                Panel(Modifier.fillMaxWidth(), surface = Surface.LEATHER) {
                    SectionHeader("Personal Purse")
                    CoinRow(w.balance)
                    Spacer(Modifier.height(4.dp))
                    RhText("Total: ${Money.format(w.balance)}", style = Theme.small, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
        if (c.background.isNotBlank() || c.description.isNotBlank()) item {
            Panel(Modifier.fillMaxWidth(), surface = Surface.PARCHMENT) {
                if (c.background.isNotBlank()) { RhText("BACKGROUND", style = Theme.label.copy(fontSize = 10.5.sp), color = Theme.goldDark); RhText(c.background) }
                if (c.description.isNotBlank()) { Spacer(Modifier.height(6.dp)); RhText("DESCRIPTION", style = Theme.label.copy(fontSize = 10.5.sp), color = Theme.goldDark); RhText(c.description) }
            }
        }
        if (dm) item { WoodButton("Remove from party", { deleting = true }, Modifier.fillMaxWidth(), kind = BtnKind.DANGER, icon = RhIcon.TRASH, compact = true) }
    }
    if (editing) CharacterEditor(c.campaignId, c) { editing = false }
    if (deleting) ConfirmDialog("Remove ${c.name}?", "Their items, skills and effects are deleted; any coins move to the party purse.", "Remove", { dispatch(Action.DeleteCharacter(newActionId(), c.id)); onDeleted() }, { deleting = false })
}

@Composable
private fun StatRow(label: String, line: com.runehearth.app.engine.StatLine) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        RhText(label, Modifier.weight(1f))
        if (line.equipment != 0) RhText("${sign(line.equipment)} gear", style = Theme.small, color = Theme.gold)
        if (line.status != 0) { Spacer(Modifier.width(6.dp)); RhText("${sign(line.status)} fx", style = Theme.small, color = if (line.status > 0) androidx.compose.ui.graphics.Color(0xFF5CB85C) else androidx.compose.ui.graphics.Color(0xFFD9453B)) }
        Spacer(Modifier.width(10.dp))
        RhText(line.total.toString(), style = Theme.heading.copy(fontSize = 18.sp))
    }
}

private fun sign(v: Int) = if (v > 0) "+$v" else "$v"

// ---------------------------------------------------------------- Skills

@Composable
private fun SkillsTab(c: GameCharacter, dm: Boolean) {
    val dao = rememberDao()
    val dispatch = rememberDispatch()
    val skills by rememberFlow(c.id, emptyList()) { dao.observeSkills(c.id) }
    var editing by remember { mutableStateOf<Skill?>(null) }
    var creating by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (dm) item { WoodButton("Add skill or spell", { creating = true }, Modifier.fillMaxWidth(), icon = RhIcon.PLUS, compact = true, kind = BtnKind.GOLD) }
        if (skills.isEmpty()) item { EmptyState(RhIcon.WAND, "No skills yet", if (dm) "Teach this character a skill or spell." else "Your Dungeon Master has not granted you any skills.") }
        items(skills, key = { it.id }) { s ->
            Panel(Modifier.fillMaxWidth(), glow = Theme.rarity(s.rarity)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    ArtSlot(AssetRef.decode(s.art), 56.dp, Theme.rarity(s.rarity))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            RhText(s.name, Modifier.weight(1f, fill = false), style = Theme.heading.copy(fontSize = 17.sp), maxLines = 1)
                            Tag(s.rarity.label, Theme.rarity(s.rarity))
                        }
                        RhText("Lv ${s.level} · ${s.kind.label}" + (if (s.power > 0) " · Power ${s.power}" else "") + (if (s.manaCost > 0) " · ${s.manaCost} MP" else "") + (if (s.cooldown > 0) " · CD ${s.cooldown}" else ""), style = Theme.small)
                        if (s.effect.isNotBlank()) RhText(s.effect, style = Theme.small, color = Theme.gold)
                        if (s.description.isNotBlank()) RhText(s.description, style = Theme.small)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WoodButton("Cast", { dispatch(Action.CastSkill(newActionId(), s.id)) }, compact = true, icon = RhIcon.WAND, enabled = c.curMp >= s.manaCost)
                    if (dm) { WoodButton("Edit", { editing = s }, compact = true, kind = BtnKind.SECONDARY); WoodButton("Delete", { dispatch(Action.DeleteSkill(newActionId(), s.id)) }, compact = true, kind = BtnKind.DANGER) }
                }
            }
        }
    }
    if (creating) SkillEditor(c.id, null) { creating = false }
    editing?.let { s -> SkillEditor(c.id, s) { editing = null } }
}

// ---------------------------------------------------------------- Gear

@Composable
private fun GearTab(c: GameCharacter, equipped: List<Item>, dm: Boolean) {
    var detail by remember { mutableStateOf<Item?>(null) }
    val slots = listOf(EquipSlot.HELMET, EquipSlot.CHESTPLATE, EquipSlot.GLOVES, EquipSlot.LEGGINGS, EquipSlot.BOOTS, EquipSlot.MAIN_HAND, EquipSlot.OFF_HAND)
    val accessories = equipped.filter { it.slot == EquipSlot.ACCESSORY }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Panel(Modifier.fillMaxWidth()) {
                SectionHeader("Armor & Weapons")
                val cells = slots.map { s -> s.label to equipped.firstOrNull { it.slot == s } } + (0 until 3).map { "Accessory ${it + 1}" to accessories.getOrNull(it) }
                cells.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { (label, item) ->
                            Column(Modifier.width(92.dp).pressable(item != null) { detail = item }, horizontalAlignment = Alignment.CenterHorizontally) {
                                ArtSlot(AssetRef.decode(item?.art ?: ""), 70.dp, item?.let { Theme.rarity(it.rarity) } ?: Theme.goldDark.copy(alpha = 0.5f))
                                RhText(item?.name ?: label, style = Theme.small.copy(fontSize = 10.5.sp), maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
        item {
            val t = equipped.fold(IntArray(5)) { a, i -> a[0] += i.bonusAtk; a[1] += i.bonusDef; a[2] += i.bonusSpd; a[3] += i.bonusHp; a[4] += i.bonusMp; a }
            Panel(Modifier.fillMaxWidth(), surface = Surface.STONE) {
                SectionHeader("Equipment Bonuses")
                KeyValue("Attack", sign(t[0])); KeyValue("Defense", sign(t[1])); KeyValue("Speed", sign(t[2])); KeyValue("Max HP", sign(t[3])); KeyValue("Max MP", sign(t[4]))
            }
        }
    }
    detail?.let { ItemDetailDialog(it, c, equipped, dm) { detail = null } }
}

// ---------------------------------------------------------------- Pack

private enum class SortMode(val label: String) { NAME("Name"), RARITY("Rarity"), KIND("Type"), QTY("Quantity") }

@Composable
private fun PackTab(c: GameCharacter, equipped: List<Item>, dm: Boolean) {
    val dao = rememberDao()
    val items by rememberFlow(c.id, emptyList()) { dao.observeItemsOf(c.id) }
    val stash by rememberFlow(c.campaignId, emptyList()) { dao.observeStash(c.campaignId) }
    val camp by rememberFlow(c.campaignId, null) { dao.observeCampaign(c.campaignId) }
    var showStash by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf<String?>(null) }
    var sort by rememberSaveable { mutableStateOf(SortMode.KIND.name) }
    var detail by remember { mutableStateOf<Item?>(null) }
    var creating by remember { mutableStateOf(false) }
    val source = if (showStash) stash else items.filter { !it.equipped }
    val shown = remember(source, query, kind, sort) {
        val q = query.trim().lowercase()
        source.filter { (kind == null || it.kind.name == kind) && (q.isEmpty() || it.name.lowercase().contains(q) || it.description.lowercase().contains(q)) }
            .sortedWith(when (SortMode.valueOf(sort)) {
                SortMode.NAME -> compareBy { it.name.lowercase() }
                SortMode.RARITY -> compareByDescending<Item> { it.rarity.ordinal }.thenBy { it.name.lowercase() }
                SortMode.KIND -> compareBy<Item> { it.kind.ordinal }.thenBy { it.name.lowercase() }
                SortMode.QTY -> compareByDescending<Item> { it.quantity }.thenBy { it.name.lowercase() }
            })
    }
    val weight = items.sumOf { it.weight * it.quantity }
    val limit = camp?.weightLimit ?: 0
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { Chip("My Pack", !showStash, { showStash = false }); Chip("Party Stash", showStash, { showStash = true }) }
            Spacer(Modifier.height(6.dp))
            RhTextField(query, { query = it }, label = "Search", modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item { Chip("All", kind == null, { kind = null }) }
                items(ItemKind.entries) { k -> Chip(k.label, kind == k.name, { kind = if (kind == k.name) null else k.name }) }
            }
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                item { RhText("SORT", style = Theme.label.copy(fontSize = 10.sp)) }
                items(SortMode.entries) { m -> Chip(m.label, sort == m.name, { sort = m.name }, color = Theme.steel) }
            }
            if (limit > 0 && !showStash) { Spacer(Modifier.height(6.dp)); StatBar("Carry weight", weight, limit, if (weight > limit) androidx.compose.ui.graphics.Color(0xFFD9453B) else Theme.gold) }
            if (dm) { Spacer(Modifier.height(6.dp)); WoodButton("Add item", { creating = true }, Modifier.fillMaxWidth(), icon = RhIcon.PLUS, compact = true, kind = BtnKind.GOLD) }
        }
        if (shown.isEmpty()) item { EmptyState(RhIcon.BAG, if (showStash) "The stash is empty" else "Nothing here", if (query.isNotBlank() || kind != null) "No items match your search or filter." else "Items you collect will appear here.") }
        items(shown, key = { it.id }) { i -> ItemRow(i) { detail = i } }
    }
    detail?.let { ItemDetailDialog(it, c, equipped, dm) { detail = null } }
    if (creating) ItemEditor(c.campaignId, if (showStash) null else c.id, null) { creating = false }
}

@Composable
fun ItemRow(i: Item, onClick: () -> Unit) {
    Panel(Modifier.fillMaxWidth(), padding = 8.dp, onClick = onClick, glow = Theme.rarity(i.rarity)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            ArtSlot(AssetRef.decode(i.art), 52.dp, Theme.rarity(i.rarity))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                RhText(i.name, style = Theme.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), maxLines = 1)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Tag(i.rarity.label, Theme.rarity(i.rarity))
                    RhText(i.kind.label + (i.slot?.let { " · ${it.label}" } ?: ""), style = Theme.small, maxLines = 1)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (i.quantity > 1) RhText("×${i.quantity}", style = Theme.heading.copy(fontSize = 16.sp))
                if (i.weight > 0) RhText("${i.weight * i.quantity} wt", style = Theme.small)
            }
        }
    }
}

@Composable
fun ItemDetailDialog(item: Item, owner: GameCharacter, equipped: List<Item>, dm: Boolean, onDismiss: () -> Unit) {
    val dao = rememberDao()
    val dispatch = rememberDispatch()
    val chars by rememberFlow(owner.campaignId, emptyList()) { dao.observeCharacters(owner.campaignId) }
    val live by rememberFlow(item.id, item) { kotlinx.coroutines.flow.flow { emit(dao.item(item.id) ?: item) } }
    val inStash = item.ownerId == null
    var moving by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var qty by remember { mutableStateOf(1) }
    val current = equipped.firstOrNull { it.slot != null && it.slot == item.slot && it.id != item.id }
    RhDialog(onDismiss, item.name, actions = { WoodButton("Close", onDismiss, kind = BtnKind.SECONDARY, compact = true) }) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                ArtSlot(AssetRef.decode(item.art), 84.dp, Theme.rarity(item.rarity))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Tag(item.rarity.label, Theme.rarity(item.rarity))
                    RhText(item.kind.label + (item.slot?.let { " · ${it.label}" } ?: ""), style = Theme.small)
                    RhText("Quantity ${item.quantity}" + if (item.weight > 0) " · Weight ${item.weight}" else "", style = Theme.small)
                    if (item.value > 0) RhText("Worth ${Money.format(item.value)}", style = Theme.small)
                }
            }
            if (item.description.isNotBlank()) RhText(item.description)
            val rows = listOf(
                Triple("Attack", item.bonusAtk, current?.bonusAtk ?: 0), Triple("Defense", item.bonusDef, current?.bonusDef ?: 0), Triple("Speed", item.bonusSpd, current?.bonusSpd ?: 0),
                Triple("Max HP", item.bonusHp, current?.bonusHp ?: 0), Triple("Max MP", item.bonusMp, current?.bonusMp ?: 0),
            ).filter { it.second != 0 || (current != null && it.third != 0) }
            if (rows.isNotEmpty()) {
                RhText(if (current != null && !item.equipped) "COMPARED TO ${current.name.uppercase()}" else "BONUSES", style = Theme.label.copy(fontSize = 10.5.sp), color = Theme.goldDark)
                rows.forEach { (n, v, cur) ->
                    val diff = v - (if (item.equipped) 0 else cur)
                    Row(Modifier.fillMaxWidth()) {
                        RhText(n, Modifier.weight(1f)); RhText(sign(v), color = Theme.ink)
                        if (current != null && !item.equipped) RhText("  (${sign(diff)})", color = if (diff > 0) androidx.compose.ui.graphics.Color(0xFF2E7D32) else if (diff < 0) androidx.compose.ui.graphics.Color(0xFFB3261E) else Theme.inkSoft)
                    }
                }
            }
            if (item.healHp > 0 || item.healMp > 0) RhText("Restores ${if (item.healHp > 0) "${item.healHp} HP " else ""}${if (item.healMp > 0) "${item.healMp} MP" else ""}", color = androidx.compose.ui.graphics.Color(0xFF2E7D32))
            Spacer(Modifier.height(4.dp))
            if (item.quantity > 1 && !item.equipped) Stepper(qty, { qty = it }, min = 1, max = item.quantity, label = "Amount to move")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (inStash && !dm) WoodButton("Take", { dispatch(Action.TransferItem(newActionId(), item.id, owner.id, qty)); onDismiss() }, compact = true, icon = RhIcon.BAG)
                if (!inStash && item.slot != null) {
                    if (item.equipped) WoodButton("Unequip", { dispatch(Action.Unequip(newActionId(), item.id)); onDismiss() }, compact = true, kind = BtnKind.SECONDARY)
                    else WoodButton("Equip", { dispatch(Action.Equip(newActionId(), item.id)); onDismiss() }, compact = true, kind = BtnKind.GOLD)
                }
                if (!inStash && (item.healHp > 0 || item.healMp > 0) && !item.equipped) WoodButton("Use", { dispatch(Action.UseItem(newActionId(), item.id)); onDismiss() }, compact = true, icon = RhIcon.POTION)
            }
            if (!item.equipped && (dm || !inStash)) WoodButton("Move…", { moving = true }, compact = true, kind = BtnKind.SECONDARY, icon = RhIcon.ARROW)
            if (dm) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WoodButton("Edit", { editing = true }, compact = true, kind = BtnKind.SECONDARY, icon = RhIcon.EDIT)
                WoodButton("Delete", { dispatch(Action.DeleteItem(newActionId(), item.id)); onDismiss() }, compact = true, kind = BtnKind.DANGER, icon = RhIcon.TRASH)
            }
        }
    }
    if (moving) RhDialog({ moving = false }, "Move ${item.name}") {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (!inStash) WoodButton("Party Stash", { dispatch(Action.TransferItem(newActionId(), item.id, null, qty)); moving = false; onDismiss() }, Modifier.fillMaxWidth(), compact = true, kind = BtnKind.SECONDARY)
            if (dm) chars.filter { it.id != item.ownerId }.forEach { ch ->
                WoodButton(ch.name, { dispatch(Action.TransferItem(newActionId(), item.id, ch.id, qty)); moving = false; onDismiss() }, Modifier.fillMaxWidth(), compact = true, kind = BtnKind.SECONDARY)
            }
        }
    }
    if (editing) ItemEditor(owner.campaignId, item.ownerId, live) { editing = false; onDismiss() }
}

// ---------------------------------------------------------------- Effects

@Composable
private fun EffectsTab(c: GameCharacter, statuses: List<StatusEffect>, dm: Boolean) {
    val dispatch = rememberDispatch()
    var adding by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (dm) item { WoodButton("Add status effect", { adding = true }, Modifier.fillMaxWidth(), icon = RhIcon.PLUS, compact = true, kind = BtnKind.GOLD) }
        if (statuses.isEmpty()) item { EmptyState(RhIcon.HEART, "No active effects", "Buffs and debuffs will appear here.") }
        items(statuses, key = { it.id }) { s -> StatusRow(s, dm) { dispatch(Action.DeleteStatus(newActionId(), s.id)) } }
    }
    if (adding) StatusEditor(c.id) { adding = false }
}

@Composable
fun StatusRow(s: StatusEffect, dm: Boolean, onRemove: () -> Unit) {
    val col = if (s.isBuff) androidx.compose.ui.graphics.Color(0xFF5CB85C) else androidx.compose.ui.graphics.Color(0xFFD9453B)
    Panel(Modifier.fillMaxWidth(), padding = 8.dp, glow = col) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RhIconView(if (s.isBuff) RhIcon.STAR else RhIcon.SKULL, col, 26.dp)
            Column(Modifier.weight(1f)) {
                RhText(s.name, style = Theme.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
                val mods = listOf("ATK" to s.modAtk, "DEF" to s.modDef, "SPD" to s.modSpd, "HP" to s.modHp, "MP" to s.modMp).filter { it.second != 0 }.joinToString("  ") { "${it.first} ${sign(it.second)}" }
                RhText((if (s.turns > 0) "${s.turns} turn(s) left" else "Until removed") + if (mods.isNotEmpty()) " · $mods" else "", style = Theme.small)
                if (s.description.isNotBlank()) RhText(s.description, style = Theme.small)
            }
            if (dm) RhIconButton(RhIcon.CLOSE, onRemove, tint = Theme.onDarkSoft, size = 18.dp)
        }
    }
}

// ---------------------------------------------------------------- Journal

@Composable
private fun JournalTab(c: GameCharacter, dm: Boolean) {
    val dao = rememberDao()
    val dispatch = rememberDispatch()
    val toaster = LocalToaster.current
    val entries by rememberFlow(c.campaignId, emptyList()) { dao.observeAllEntries(c.campaignId) }
    val rolls by rememberFlow(c.campaignId, emptyList()) { dao.observeRolls(c.campaignId, 60) }
    var notes by remember(c.id) { mutableStateOf(c.notes) }
    var seenRev by remember(c.id) { mutableStateOf(c.rev) }
    val mine = entries.filter { (it.ownerId == c.id || (it.ownerId == null && it.isPublic)) }
    val quests = mine.filter { it.type == EntryType.QUEST }
    val achievements = mine.filter { it.type == EntryType.ACHIEVEMENT }
    val myRolls = rolls.filter { it.actor == c.id }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Panel(Modifier.fillMaxWidth(), surface = Surface.PARCHMENT) {
                RhText("PERSONAL NOTES", style = Theme.label.copy(fontSize = 10.5.sp), color = Theme.goldDark)
                RhTextField(notes, { notes = it }, singleLine = false, minLines = 4, onDark = false)
                Spacer(Modifier.height(6.dp))
                WoodButton("Save notes", { dispatch(Action.UpdateNotes(newActionId(), c.id, notes, seenRev)); seenRev = c.rev + 1; toaster.show("Notes saved") }, compact = true, enabled = notes != c.notes)
            }
        }
        item { SectionHeader("Quest Log") }
        if (quests.isEmpty()) item { RhText("No quests yet.", style = Theme.small, color = Theme.onDarkSoft) }
        items(quests, key = { it.id }) { q ->
            Panel(Modifier.fillMaxWidth(), surface = Surface.PARCHMENT, padding = 10.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RhText(q.title, Modifier.weight(1f), style = Theme.heading.copy(fontSize = 16.sp))
                    Tag(q.status.label, when (q.status) { EntryStatus.ACTIVE -> androidx.compose.ui.graphics.Color(0xFFB7791F); EntryStatus.COMPLETED -> androidx.compose.ui.graphics.Color(0xFF2E7D32); else -> androidx.compose.ui.graphics.Color(0xFFB3261E) })
                }
                if (q.body.isNotBlank()) RhText(q.body)
                if (q.rewardBronze > 0) RhText("Reward: ${Money.format(q.rewardBronze)}", style = Theme.small, color = Theme.goldDark)
            }
        }
        item { SectionHeader("Achievements") }
        if (achievements.isEmpty()) item { RhText("No achievements yet.", style = Theme.small, color = Theme.onDarkSoft) }
        items(achievements, key = { it.id }) { a ->
            Panel(Modifier.fillMaxWidth(), padding = 10.dp) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { RhIconView(RhIcon.CROWN, Theme.gold, 22.dp); Column { RhText(a.title, style = Theme.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)); if (a.body.isNotBlank()) RhText(a.body, style = Theme.small) } } }
        }
        item { SectionHeader("Recent Rolls") }
        if (myRolls.isEmpty()) item { RhText("No rolls yet.", style = Theme.small, color = Theme.onDarkSoft) }
        items(myRolls.take(15), key = { it.id }) { r -> RollLine(r) }
    }
}

@Composable
fun RollLine(r: com.runehearth.app.data.RollRecord) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RhIconView(RhIcon.DICE, Theme.gold, 18.dp)
        RhText("${r.expr}${if (r.label.isNotBlank()) " · ${r.label}" else ""}", Modifier.weight(1f), style = Theme.small, maxLines = 1)
        RhText("[${r.dice}]", style = Theme.small, maxLines = 1)
        RhText(r.total.toString(), style = Theme.heading.copy(fontSize = 16.sp))
    }
}
