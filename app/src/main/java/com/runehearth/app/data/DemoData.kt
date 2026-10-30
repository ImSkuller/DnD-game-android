package com.runehearth.app.data

import com.runehearth.app.RunehearthApp
import com.runehearth.app.art.ArtCategory
import com.runehearth.app.art.AssetRef
import com.runehearth.app.core.Money
import com.runehearth.app.engine.Action
import com.runehearth.app.engine.Actor
import com.runehearth.app.session.newActionId
import com.runehearth.app.session.newEntityId
import com.runehearth.app.ui.sheet.ItemTemplates

/** Builds a ready-to-play sample campaign through the normal action engine (also a handy manual-test fixture). */
object DemoData {
    suspend fun create(app: RunehearthApp, mode: CampaignMode = CampaignMode.LOCAL): String {
        val cid = app.createCampaign("The Sunken Crypt (Sample)", mode)
        val dm = Actor.dm(app.prefs.deviceId())
        suspend fun run(a: Action) { app.engine.execute(cid, dm, a) }
        fun tpl(n: String) = ItemTemplates.all.first { it.name == n }

        class Hero(val name: String, val cls: String, val race: String, val job: String, val art: AssetRef, val stats: List<Int>, val gold: Long)
        val heroes = listOf(
            Hero("Aelwyn Stormcaller", "Wizard", "Elf", "Court Mage", AssetRef(ArtCategory.HELMET, 14, 12, true), listOf(26, 40, 3, 2, 6), 120),
            Hero("Borin Ironhand", "Fighter", "Dwarf", "Blacksmith", AssetRef(ArtCategory.HELMET, 7, 15, false), listOf(48, 6, 8, 7, 3), 85),
            Hero("Kira Nightwhisper", "Rogue", "Halfling", "Pickpocket", AssetRef(ArtCategory.HELMET, 9, 16, false), listOf(32, 12, 6, 3, 9), 240),
        )
        val ids = heroes.map { h ->
            val id = newEntityId()
            run(Action.UpsertCharacter(newActionId(), GameCharacter(id, cid, h.name, h.art.encode(), className = h.cls, race = h.race, job = h.job,
                baseHp = h.stats[0], baseMp = h.stats[1], baseAtk = h.stats[2], baseDef = h.stats[3], baseSpd = h.stats[4], curHp = h.stats[0], curMp = h.stats[1],
                background = "Drawn to the crypt beneath Hollowmere by rumours of a lost relic.", description = "A seasoned adventurer with a sharp wit.")))
            run(Action.AdjustWallet(newActionId(), Wallet.personalId(id), h.gold * 100 + 37, "Starting funds"))
            id
        }
        run(Action.AdjustWallet(newActionId(), Wallet.partyId(cid), Money.of(holyGold = 1, gold = 12, silver = 5), "Party fund"))
        val gear = listOf(
            listOf("Archmage Staff", "Apprentice Robe", "Amulet of the Sage", "Mana Draught", "Minor Healing Potion", "Fire Rune", "Tome of Embers"),
            listOf("Knight's Longsword", "Crusader Shield", "Steel Great Helm", "Chainmail Hauberk", "Plate Greaves", "Iron Gauntlets", "Greater Healing Potion"),
            listOf("Whisper Dagger", "Thief's Gloves", "Boots of Swiftness", "Leather Cap", "Minor Healing Potion", "Iron Key", "Torn Map"),
        )
        for ((i, names) in gear.withIndex()) for (n in names) {
            val item = tpl(n).toItem(cid, ids[i], if (n.contains("Potion")) 3 else 1)
            run(Action.UpsertItem(newActionId(), item, mergeStacks = true))
        }
        // equip the first few pieces of gear
        for ((i, names) in gear.withIndex()) for (n in names.take(if (i == 1) 6 else 3)) {
            val it = app.db.dao().itemsOf(ids[i]).firstOrNull { x -> x.name == n && x.slot != null } ?: continue
            run(Action.Equip(newActionId(), it.id))
        }
        listOf("Travel Rations", "Torch", "Treasure Chest", "Raw Sapphire").forEach { run(Action.UpsertItem(newActionId(), tpl(it).toItem(cid, null, 2), mergeStacks = true)) }
        val skills = listOf(
            listOf(Skill("", ids[0], "Fireball", 3, "A roaring sphere of flame that bursts on impact.", Rarity.RARE, 12, 2, SkillKind.DAMAGE, 24, "Burns for 2 turns", AssetRef(ArtCategory.RUNE, 0, 1, true).encode()),
                Skill("", ids[0], "Frost Ward", 2, "Ice shards orbit the caster.", Rarity.UNCOMMON, 8, 3, SkillKind.BUFF, 0, "+3 DEF for 3 turns", AssetRef(ArtCategory.RUNE, 12, 9, true).encode())),
            listOf(Skill("", ids[1], "Shield Bash", 2, "Slam an enemy and stagger them.", Rarity.COMMON, 0, 2, SkillKind.DAMAGE, 8, "Stuns for 1 turn", AssetRef(ArtCategory.OFFHAND, 3, 15, false).encode())),
            listOf(Skill("", ids[2], "Shadowstep", 3, "Vanish and reappear behind a foe.", Rarity.EPIC, 6, 3, SkillKind.UTILITY, 0, "Next attack deals double damage", AssetRef(ArtCategory.RUNE, 8, 12, true).encode())),
        )
        skills.forEachIndexed { i, l -> l.forEach { s -> run(Action.UpsertSkill(newActionId(), s.copy(id = newEntityId(), characterId = ids[i]))) } }
        run(Action.UpsertStatus(newActionId(), StatusEffect(newEntityId(), ids[1], "Blessed", "A cleric's blessing.", true, 4, modAtk = 2, modDef = 2)))
        run(Action.UpsertEntry(newActionId(), Entry(newEntityId(), cid, EntryType.QUEST, "Recover the Sunken Relic", "Descend into the flooded crypt below Hollowmere and bring back the Tideheart Amulet.", rewardBronze = Money.of(gold = 50), isPublic = true)))
        run(Action.UpsertEntry(newActionId(), Entry(newEntityId(), cid, EntryType.NPC, "Mayor Orlan Vex", "Plump, nervous, and hiding something about the crypt.", art = AssetRef(ArtCategory.MONSTER, 17, 17, false).encode())))
        run(Action.UpsertEntry(newActionId(), Entry(newEntityId(), cid, EntryType.LOCATION, "Hollowmere", "A fog-bound lake town. The crypt entrance lies beneath the old chapel.", isPublic = true)))
        run(Action.UpsertEntry(newActionId(), Entry(newEntityId(), cid, EntryType.ACHIEVEMENT, "First Blood", "Won the first fight of the campaign.", ownerId = ids[1])))
        val enc = newEntityId()
        run(Action.UpsertEncounter(newActionId(), Encounter(enc, cid, "Skeletons in the Flooded Hall")))
        run(Action.AddPartyToEncounter(newActionId(), enc))
        for ((n, art) in listOf("Skeleton Warrior" to 3, "Skeleton Archer" to 3, "Drowned Ghost" to 4)) {
            run(Action.UpsertCombatant(newActionId(), Combatant(newEntityId(), enc, cid, null, n, 0, 18, 18, 2, false, AssetRef(ArtCategory.MONSTER, art, if (art == 4) 8 else 17, art == 4).encode())))
        }
        run(Action.RollInitiative(newActionId(), enc))
        repeat(2) { run(Action.RollDice(newActionId(), com.runehearth.app.core.RollSpec(1, 20, 2), "Perception check", true)) }
        return cid
    }
}
