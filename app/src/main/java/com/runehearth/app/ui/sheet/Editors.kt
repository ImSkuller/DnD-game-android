package com.runehearth.app.ui.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runehearth.app.art.ArtCategory
import com.runehearth.app.data.EquipSlot
import com.runehearth.app.data.GameCharacter
import com.runehearth.app.data.Item
import com.runehearth.app.data.ItemKind
import com.runehearth.app.data.Rarity
import com.runehearth.app.data.Skill
import com.runehearth.app.data.SkillKind
import com.runehearth.app.data.StatusEffect
import com.runehearth.app.engine.Action
import com.runehearth.app.session.newActionId
import com.runehearth.app.session.newEntityId
import com.runehearth.app.ui.assets.ArtField
import com.runehearth.app.ui.components.BtnKind
import com.runehearth.app.ui.components.Chip
import com.runehearth.app.ui.components.RhDialog
import com.runehearth.app.ui.components.RhNumberField
import com.runehearth.app.ui.components.RhText
import com.runehearth.app.ui.components.RhTextField
import com.runehearth.app.ui.components.WoodButton
import com.runehearth.app.ui.rememberDispatch
import com.runehearth.app.ui.theme.Theme

@Composable
fun IntField(label: String, value: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    RhNumberField(value.toLong().coerceAtLeast(0), { onChange(it.toInt()) }, modifier, label, max = 99999)
}

/** Signed integer input as a stepper pair for bonuses that may be negative. */
@Composable
fun SignedField(label: String, value: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    com.runehearth.app.ui.components.Stepper(value, onChange, modifier, min = -999, max = 999, label = label)
}

@Composable
private fun <T> EnumChips(label: String, all: List<T>, selected: T, name: (T) -> String, onPick: (T) -> Unit) {
    RhText(label.uppercase(), style = Theme.label.copy(fontSize = 10.5.sp), color = Theme.goldDark)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(all) { Chip(name(it), it == selected, { onPick(it) }) } }
}

@Composable
fun CharacterEditor(campaignId: String, initial: GameCharacter?, onDismiss: () -> Unit) {
    val dispatch = rememberDispatch()
    var c by remember { mutableStateOf(initial ?: GameCharacter(newEntityId(), campaignId, "")) }
    RhDialog(onDismiss, if (initial == null) "New Character" else "Edit Character", modifier = Modifier.height(640.dp), actions = {
        WoodButton("Cancel", onDismiss, kind = BtnKind.SECONDARY, compact = true)
        WoodButton("Save", {
            dispatch(Action.UpsertCharacter(newActionId(), c.copy(curHp = if (initial == null) c.baseHp else c.curHp, curMp = if (initial == null) c.baseMp else c.curMp), initial?.rev ?: -1)); onDismiss()
        }, kind = BtnKind.GOLD, compact = true, enabled = c.name.isNotBlank())
    }) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!com.runehearth.app.ui.LocalSession.current.isDm) RhText("You can change your own stats. Your Dungeon Master is notified when you do.", style = Theme.small)
            ArtField("Portrait", c.art, listOf(ArtCategory.HELMET, ArtCategory.MONSTER), { c = c.copy(art = it) })
            RhTextField(c.name, { c = c.copy(name = it.take(60)) }, label = "Name", onDark = false)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RhTextField(c.className, { c = c.copy(className = it.take(40)) }, Modifier.weight(1f), "Class", onDark = false)
                RhTextField(c.job, { c = c.copy(job = it.take(40)) }, Modifier.weight(1f), "Job", onDark = false)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RhTextField(c.race, { c = c.copy(race = it.take(40)) }, Modifier.weight(1f), "Race", onDark = false)
                IntField("Level", c.level, { c = c.copy(level = it.coerceIn(1, 100)) }, Modifier.weight(1f))
            }
            RhText("STATS", style = Theme.label.copy(fontSize = 10.5.sp), color = Theme.goldDark)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IntField("Health", c.baseHp, { c = c.copy(baseHp = it) }, Modifier.weight(1f)); IntField("Mana", c.baseMp, { c = c.copy(baseMp = it) }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IntField("Attack", c.baseAtk, { c = c.copy(baseAtk = it) }, Modifier.weight(1f)); IntField("Defense", c.baseDef, { c = c.copy(baseDef = it) }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IntField("Speed", c.baseSpd, { c = c.copy(baseSpd = it) }, Modifier.weight(1f)); IntField("Luck", c.baseLuck, { c = c.copy(baseLuck = it) }, Modifier.weight(1f))
            }
            RhTextField(c.background, { c = c.copy(background = it) }, label = "Background", singleLine = false, minLines = 2, onDark = false)
            RhTextField(c.description, { c = c.copy(description = it) }, label = "Description", singleLine = false, minLines = 2, onDark = false)
            if (com.runehearth.app.ui.LocalSession.current.isDm) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Player character", !c.isNpc, { c = c.copy(isNpc = false) }); Chip("NPC (DM only)", c.isNpc, { c = c.copy(isNpc = true) })
            }
        }
    }
}

@Composable
fun ItemEditor(campaignId: String, ownerId: String?, initial: Item?, onDismiss: () -> Unit) {
    val dispatch = rememberDispatch()
    var it0 by remember { mutableStateOf(initial ?: Item(newEntityId(), campaignId, ownerId, "")) }
    val i = it0
    RhDialog(onDismiss, if (initial == null) "New Item" else "Edit Item", modifier = Modifier.height(660.dp), actions = {
        WoodButton("Cancel", onDismiss, kind = BtnKind.SECONDARY, compact = true)
        WoodButton("Save", { dispatch(Action.UpsertItem(newActionId(), i, mergeStacks = initial == null)); onDismiss() }, kind = BtnKind.GOLD, compact = true, enabled = i.name.isNotBlank())
    }) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!com.runehearth.app.ui.LocalSession.current.isDm) RhText("Your Dungeon Master is notified when you add or change items.", style = Theme.small)
            if (initial == null) {
                RhText("QUICK TEMPLATES", style = Theme.label.copy(fontSize = 10.5.sp), color = Theme.goldDark)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ItemTemplates.all) { t -> Chip(t.name, false, { it0 = t.toItem(campaignId, ownerId).copy(id = i.id) }) }
                }
            }
            ArtField("Image", i.art, ArtCategory.entries.filter { it != ArtCategory.MONSTER }, { it0 = i.copy(art = it) })
            RhTextField(i.name, { it0 = i.copy(name = it.take(60)) }, label = "Name", onDark = false)
            EnumChips("Type", ItemKind.entries, i.kind, { it.label }) { k -> it0 = i.copy(kind = k, slot = if (k != ItemKind.WEAPON && k != ItemKind.ARMOR && k != ItemKind.ACCESSORY) null else i.slot) }
            if (i.kind == ItemKind.WEAPON || i.kind == ItemKind.ARMOR || i.kind == ItemKind.ACCESSORY) {
                val slots: List<EquipSlot?> = listOf(null) + EquipSlot.entries
                EnumChips("Equip slot", slots, i.slot, { it?.label ?: "None" }) { s -> it0 = i.copy(slot = s) }
            }
            EnumChips("Rarity", Rarity.entries, i.rarity, { it.label }) { r -> it0 = i.copy(rarity = r) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IntField("Quantity", i.quantity, { it0 = i.copy(quantity = it.coerceAtLeast(1)) }, Modifier.weight(1f)); IntField("Weight", i.weight, { it0 = i.copy(weight = it) }, Modifier.weight(1f))
            }
            RhText("BONUSES WHEN EQUIPPED", style = Theme.label.copy(fontSize = 10.5.sp), color = Theme.goldDark)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SignedField("Attack", i.bonusAtk, { it0 = i.copy(bonusAtk = it) }, Modifier.weight(1f)); SignedField("Defense", i.bonusDef, { it0 = i.copy(bonusDef = it) }, Modifier.weight(1f)); SignedField("Speed", i.bonusSpd, { it0 = i.copy(bonusSpd = it) }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SignedField("Health", i.bonusHp, { it0 = i.copy(bonusHp = it) }, Modifier.weight(1f)); SignedField("Mana", i.bonusMp, { it0 = i.copy(bonusMp = it) }, Modifier.weight(1f)); SignedField("Luck", i.bonusLuck, { it0 = i.copy(bonusLuck = it) }, Modifier.weight(1f))
            }
            RhText("WHEN USED", style = Theme.label.copy(fontSize = 10.5.sp), color = Theme.goldDark)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IntField("Heal Health", i.healHp, { it0 = i.copy(healHp = it) }, Modifier.weight(1f)); IntField("Restore Mana", i.healMp, { it0 = i.copy(healMp = it) }, Modifier.weight(1f))
            }
            RhTextField(i.description, { it0 = i.copy(description = it) }, label = "Description", singleLine = false, minLines = 2, onDark = false)
        }
    }
}

@Composable
fun SkillEditor(characterId: String, initial: Skill?, onDismiss: () -> Unit) {
    val dispatch = rememberDispatch()
    var s by remember { mutableStateOf(initial ?: Skill(newEntityId(), characterId, "")) }
    RhDialog(onDismiss, if (initial == null) "New Skill" else "Edit Skill", modifier = Modifier.height(620.dp), actions = {
        WoodButton("Cancel", onDismiss, kind = BtnKind.SECONDARY, compact = true)
        WoodButton("Save", { dispatch(Action.UpsertSkill(newActionId(), s)); onDismiss() }, kind = BtnKind.GOLD, compact = true, enabled = s.name.isNotBlank())
    }) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ArtField("Icon", s.art, listOf(ArtCategory.RUNE, ArtCategory.BOOK, ArtCategory.POTION, ArtCategory.WEAPON, ArtCategory.ACCESSORY, ArtCategory.HERB, ArtCategory.MISC), { s = s.copy(art = it) })
            RhTextField(s.name, { s = s.copy(name = it.take(60)) }, label = "Name", onDark = false)
            EnumChips("Kind", SkillKind.entries, s.kind, { it.label }) { k -> s = s.copy(kind = k) }
            EnumChips("Rarity", Rarity.entries, s.rarity, { it.label }) { r -> s = s.copy(rarity = r) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IntField("Level", s.level, { s = s.copy(level = it.coerceAtLeast(1)) }, Modifier.weight(1f)); IntField("Mana cost", s.manaCost, { s = s.copy(manaCost = it) }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IntField("Cooldown (turns)", s.cooldown, { s = s.copy(cooldown = it) }, Modifier.weight(1f)); IntField("Power (dmg/heal)", s.power, { s = s.copy(power = it) }, Modifier.weight(1f))
            }
            RhTextField(s.effect, { s = s.copy(effect = it.take(300)) }, label = "Stat / special effect", onDark = false)
            RhTextField(s.description, { s = s.copy(description = it) }, label = "Description", singleLine = false, minLines = 3, onDark = false)
        }
    }
}

@Composable
fun StatusEditor(targetId: String, onDismiss: () -> Unit) {
    val dispatch = rememberDispatch()
    var s by remember { mutableStateOf(StatusEffect(newEntityId(), targetId, "")) }
    val presets = listOf(
        StatusEffect("", targetId, "Poisoned", "Takes damage over time.", false, 3, modAtk = -1),
        StatusEffect("", targetId, "Blessed", "Divine favour.", true, 5, modAtk = 2, modDef = 2),
        StatusEffect("", targetId, "Slowed", "Moves sluggishly.", false, 3, modSpd = -3),
        StatusEffect("", targetId, "Hasted", "Quickened.", true, 3, modSpd = 4),
        StatusEffect("", targetId, "Stunned", "Cannot act.", false, 1, modDef = -2, modSpd = -99),
        StatusEffect("", targetId, "Shielded", "A ward absorbs blows.", true, 3, modDef = 5),
        StatusEffect("", targetId, "Weakened", "Strength sapped.", false, 3, modAtk = -3),
        StatusEffect("", targetId, "Lucky", "Fortune favours you.", true, 3, modLuck = 5),
        StatusEffect("", targetId, "Cursed", "Bad luck follows.", false, 3, modLuck = -5),
    )
    RhDialog(onDismiss, "Add Status Effect", modifier = Modifier.height(560.dp), actions = {
        WoodButton("Cancel", onDismiss, kind = BtnKind.SECONDARY, compact = true)
        WoodButton("Apply", { dispatch(Action.UpsertStatus(newActionId(), s)); onDismiss() }, kind = BtnKind.GOLD, compact = true, enabled = s.name.isNotBlank())
    }) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RhText("PRESETS", style = Theme.label.copy(fontSize = 10.5.sp), color = Theme.goldDark)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(presets) { p -> Chip(p.name, false, { s = p.copy(id = s.id) }) } }
            RhTextField(s.name, { s = s.copy(name = it.take(60)) }, label = "Name", onDark = false)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Chip("Buff", s.isBuff, { s = s.copy(isBuff = true) }); Chip("Debuff", !s.isBuff, { s = s.copy(isBuff = false) }) }
            IntField("Duration in turns (0 = until removed)", s.turns, { s = s.copy(turns = it) })
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SignedField("Attack", s.modAtk, { s = s.copy(modAtk = it) }, Modifier.weight(1f)); SignedField("Defense", s.modDef, { s = s.copy(modDef = it) }, Modifier.weight(1f)); SignedField("Speed", s.modSpd, { s = s.copy(modSpd = it) }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SignedField("Luck", s.modLuck, { s = s.copy(modLuck = it) }, Modifier.weight(1f)); SignedField("Health", s.modHp, { s = s.copy(modHp = it) }, Modifier.weight(1f)); SignedField("Mana", s.modMp, { s = s.copy(modMp = it) }, Modifier.weight(1f))
            }
            RhTextField(s.description, { s = s.copy(description = it) }, label = "Description", onDark = false)
            Spacer(Modifier.height(2.dp))
        }
    }
}
