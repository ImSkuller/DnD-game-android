package com.runehearth.app.ui.sheet

import com.runehearth.app.art.ArtCategory
import com.runehearth.app.art.AssetRef
import com.runehearth.app.data.EquipSlot
import com.runehearth.app.data.Item
import com.runehearth.app.data.ItemKind
import com.runehearth.app.data.Rarity
import com.runehearth.app.session.newEntityId

/** Ready-made item presets for quick creation and the loot generator. */
data class ItemTemplate(
    val name: String, val kind: ItemKind, val slot: EquipSlot?, val rarity: Rarity, val art: AssetRef, val description: String,
    val atk: Int = 0, val def: Int = 0, val spd: Int = 0, val hp: Int = 0, val mp: Int = 0,
    val healHp: Int = 0, val healMp: Int = 0, val weight: Int = 1, val value: Long = 0,
) {
    fun toItem(campaignId: String, ownerId: String?, quantity: Int = 1) = Item(
        id = newEntityId(), campaignId = campaignId, ownerId = ownerId, name = name, kind = kind, slot = slot, rarity = rarity,
        description = description, quantity = quantity, weight = weight, art = art.encode(), bonusHp = hp, bonusMp = mp, bonusAtk = atk,
        bonusDef = def, bonusSpd = spd, healHp = healHp, healMp = healMp, value = value,
    )
}

object ItemTemplates {
    private fun a(c: ArtCategory, d: Int, color: Int, glow: Boolean = false) = AssetRef(c, d, color, glow)

    val all: List<ItemTemplate> = listOf(
        ItemTemplate("Iron Shortsword", ItemKind.WEAPON, EquipSlot.MAIN_HAND, Rarity.COMMON, a(ArtCategory.WEAPON, 0, 15), "A plain, dependable blade.", atk = 3, weight = 3, value = 60),
        ItemTemplate("Knight's Longsword", ItemKind.WEAPON, EquipSlot.MAIN_HAND, Rarity.UNCOMMON, a(ArtCategory.WEAPON, 1, 9), "Balanced steel with a leather-wrapped hilt.", atk = 6, weight = 4, value = 220),
        ItemTemplate("Emberfang", ItemKind.WEAPON, EquipSlot.MAIN_HAND, Rarity.EPIC, a(ArtCategory.WEAPON, 2, 1, true), "A broadsword that smoulders with a hearth's heat.", atk = 12, spd = -1, weight = 6, value = 1800),
        ItemTemplate("Whisper Dagger", ItemKind.WEAPON, EquipSlot.MAIN_HAND, Rarity.RARE, a(ArtCategory.WEAPON, 5, 16), "Quick and quiet.", atk = 5, spd = 3, weight = 1, value = 500),
        ItemTemplate("Dwarven Battle Axe", ItemKind.WEAPON, EquipSlot.MAIN_HAND, Rarity.UNCOMMON, a(ArtCategory.WEAPON, 7, 18), "Heavy and brutal.", atk = 8, spd = -1, weight = 7, value = 300),
        ItemTemplate("Hunter's Bow", ItemKind.WEAPON, EquipSlot.MAIN_HAND, Rarity.COMMON, a(ArtCategory.WEAPON, 16, 6), "A yew bow with a waxed string.", atk = 4, spd = 1, weight = 2, value = 90),
        ItemTemplate("Archmage Staff", ItemKind.WEAPON, EquipSlot.MAIN_HAND, Rarity.LEGENDARY, a(ArtCategory.WEAPON, 18, 12, true), "Its orb hums with stored mana.", atk = 4, mp = 25, weight = 4, value = 5000),
        ItemTemplate("Oak Buckler", ItemKind.ARMOR, EquipSlot.OFF_HAND, Rarity.COMMON, a(ArtCategory.OFFHAND, 15, 18), "Small and nimble.", def = 2, weight = 3, value = 50),
        ItemTemplate("Crusader Shield", ItemKind.ARMOR, EquipSlot.OFF_HAND, Rarity.RARE, a(ArtCategory.OFFHAND, 1, 9, true), "Blessed by a forgotten order.", def = 6, hp = 10, weight = 6, value = 700),
        ItemTemplate("Leather Cap", ItemKind.ARMOR, EquipSlot.HELMET, Rarity.COMMON, a(ArtCategory.HELMET, 4, 18), "Boiled leather.", def = 1, weight = 1, value = 30),
        ItemTemplate("Steel Great Helm", ItemKind.ARMOR, EquipSlot.HELMET, Rarity.UNCOMMON, a(ArtCategory.HELMET, 2, 15), "Protects well, limits sight.", def = 4, spd = -1, weight = 4, value = 260),
        ItemTemplate("Winged Circlet", ItemKind.ARMOR, EquipSlot.HELMET, Rarity.EPIC, a(ArtCategory.HELMET, 12, 3, true), "Light as a feather.", def = 3, spd = 3, mp = 8, weight = 1, value = 1500),
        ItemTemplate("Chainmail Hauberk", ItemKind.ARMOR, EquipSlot.CHESTPLATE, Rarity.UNCOMMON, a(ArtCategory.CHEST_ARMOR, 2, 15), "Thousands of tiny rings.", def = 5, weight = 8, value = 350),
        ItemTemplate("Dragonscale Plate", ItemKind.ARMOR, EquipSlot.CHESTPLATE, Rarity.LEGENDARY, a(ArtCategory.CHEST_ARMOR, 5, 0, true), "Forged from the hide of a wyrm.", def = 14, hp = 30, weight = 12, value = 6000),
        ItemTemplate("Apprentice Robe", ItemKind.ARMOR, EquipSlot.CHESTPLATE, Rarity.COMMON, a(ArtCategory.CHEST_ARMOR, 3, 10), "Embroidered with faint runes.", def = 1, mp = 10, weight = 2, value = 70),
        ItemTemplate("Iron Gauntlets", ItemKind.ARMOR, EquipSlot.GLOVES, Rarity.COMMON, a(ArtCategory.GLOVES, 0, 15), "Dented but sturdy.", def = 2, weight = 2, value = 55),
        ItemTemplate("Thief's Gloves", ItemKind.ARMOR, EquipSlot.GLOVES, Rarity.RARE, a(ArtCategory.GLOVES, 1, 16), "Silent fingers.", spd = 3, atk = 1, weight = 1, value = 480),
        ItemTemplate("Plate Greaves", ItemKind.ARMOR, EquipSlot.LEGGINGS, Rarity.UNCOMMON, a(ArtCategory.LEGGINGS, 0, 15), "Knee-guarded steel.", def = 4, weight = 6, value = 240),
        ItemTemplate("Traveller's Trousers", ItemKind.ARMOR, EquipSlot.LEGGINGS, Rarity.COMMON, a(ArtCategory.LEGGINGS, 1, 18), "Well patched.", def = 1, spd = 1, weight = 1, value = 25),
        ItemTemplate("Leather Boots", ItemKind.ARMOR, EquipSlot.BOOTS, Rarity.COMMON, a(ArtCategory.BOOTS, 0, 18), "Made for walking.", def = 1, spd = 1, weight = 2, value = 40),
        ItemTemplate("Boots of Swiftness", ItemKind.ARMOR, EquipSlot.BOOTS, Rarity.EPIC, a(ArtCategory.BOOTS, 15, 8, true), "Wings flutter at the heel.", def = 2, spd = 6, weight = 2, value = 1600),
        ItemTemplate("Band of Vitality", ItemKind.ACCESSORY, EquipSlot.ACCESSORY, Rarity.UNCOMMON, a(ArtCategory.ACCESSORY, 1, 0), "Warm to the touch.", hp = 12, weight = 0, value = 300),
        ItemTemplate("Amulet of the Sage", ItemKind.ACCESSORY, EquipSlot.ACCESSORY, Rarity.RARE, a(ArtCategory.ACCESSORY, 2, 10, true), "Whispers forgotten lore.", mp = 15, weight = 0, value = 900),
        ItemTemplate("Ironbark Charm", ItemKind.ACCESSORY, EquipSlot.ACCESSORY, Rarity.COMMON, a(ArtCategory.ACCESSORY, 13, 17), "A skull charm for luck.", def = 1, weight = 0, value = 45),
        ItemTemplate("Minor Healing Potion", ItemKind.POTION, null, Rarity.COMMON, a(ArtCategory.POTION, 0, 0), "Restores a little health.", healHp = 15, weight = 0, value = 25),
        ItemTemplate("Greater Healing Potion", ItemKind.POTION, null, Rarity.UNCOMMON, a(ArtCategory.POTION, 7, 0, true), "Restores a great deal of health.", healHp = 45, weight = 0, value = 120),
        ItemTemplate("Mana Draught", ItemKind.POTION, null, Rarity.COMMON, a(ArtCategory.POTION, 5, 10), "Restores magical energy.", healMp = 20, weight = 0, value = 40),
        ItemTemplate("Elixir of Life", ItemKind.POTION, null, Rarity.LEGENDARY, a(ArtCategory.POTION, 13, 3, true), "Restores health and mana.", healHp = 100, healMp = 50, weight = 0, value = 2500),
        ItemTemplate("Fire Rune", ItemKind.RUNE, null, Rarity.UNCOMMON, a(ArtCategory.RUNE, 0, 1, true), "Glows with inner flame.", weight = 0, value = 150),
        ItemTemplate("Ward Rune", ItemKind.RUNE, null, Rarity.RARE, a(ArtCategory.RUNE, 7, 9, true), "Used to protect doors and camps.", weight = 0, value = 400),
        ItemTemplate("Tome of Embers", ItemKind.BOOK, null, Rarity.RARE, a(ArtCategory.BOOK, 4, 1), "A grimoire with a burnt cover.", weight = 2, value = 800),
        ItemTemplate("Field Guide to Herbs", ItemKind.BOOK, null, Rarity.COMMON, a(ArtCategory.BOOK, 0, 5), "Dog-eared and stained.", weight = 1, value = 35),
        ItemTemplate("Moonpetal", ItemKind.HERB, null, Rarity.UNCOMMON, a(ArtCategory.HERB, 11, 14), "Blooms only at night.", weight = 0, value = 20),
        ItemTemplate("Redcap Mushroom", ItemKind.HERB, null, Rarity.COMMON, a(ArtCategory.HERB, 2, 0), "Handle with care.", weight = 0, value = 5),
        ItemTemplate("Iron Ingot", ItemKind.MATERIAL, null, Rarity.COMMON, a(ArtCategory.MATERIAL, 0, 15), "Ready for the forge.", weight = 1, value = 10),
        ItemTemplate("Raw Sapphire", ItemKind.MATERIAL, null, Rarity.RARE, a(ArtCategory.MATERIAL, 3, 10, true), "Uncut but brilliant.", weight = 0, value = 600),
        ItemTemplate("Dragon Scale", ItemKind.MATERIAL, null, Rarity.EPIC, a(ArtCategory.MATERIAL, 13, 1, true), "Warm and impossibly hard.", weight = 0, value = 1200),
        ItemTemplate("Iron Key", ItemKind.QUEST, null, Rarity.COMMON, a(ArtCategory.MISC, 0, 15), "Opens something, somewhere.", weight = 0, value = 0),
        ItemTemplate("Torn Map", ItemKind.QUEST, null, Rarity.UNCOMMON, a(ArtCategory.MISC, 5, 17), "Half of a treasure map.", weight = 0, value = 0),
        ItemTemplate("Travel Rations", ItemKind.CONSUMABLE, null, Rarity.COMMON, a(ArtCategory.MISC, 15, 18), "Dry bread and salted meat.", healHp = 5, weight = 1, value = 5),
        ItemTemplate("Torch", ItemKind.MISC, null, Rarity.COMMON, a(ArtCategory.MISC, 2, 1), "Burns for an hour.", weight = 1, value = 2),
        ItemTemplate("Treasure Chest", ItemKind.MISC, null, Rarity.UNCOMMON, a(ArtCategory.CHEST, 5, 3), "Heavy with promise.", weight = 10, value = 0),
    )

    fun byKind(k: ItemKind?) = if (k == null) all else all.filter { it.kind == k }
}
