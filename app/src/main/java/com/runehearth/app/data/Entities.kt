package com.runehearth.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/*
 * Persistent models. Every table uses stable string ids (UUID) and a [rev] counter that increments on each change, so
 * updates are UPDATE/UPSERT on the same row (never duplicate rows). The same classes are used on the wire for sync.
 * See docs/implementation_plan.md §4 for the schema description.
 */

enum class CampaignMode { LOCAL, COOP }
enum class Rarity(val label: String) { COMMON("Common"), UNCOMMON("Uncommon"), RARE("Rare"), EPIC("Epic"), LEGENDARY("Legendary"), MYTHIC("Mythic") }
enum class ItemKind(val label: String) {
    WEAPON("Weapon"), ARMOR("Armor"), ACCESSORY("Accessory"), POTION("Potion"), RUNE("Rune"), BOOK("Book"),
    MATERIAL("Material"), HERB("Herb"), QUEST("Quest"), CONSUMABLE("Consumable"), MISC("Misc");
}
enum class EquipSlot(val label: String) {
    HELMET("Helmet"), CHESTPLATE("Chestplate"), GLOVES("Gloves"), LEGGINGS("Leggings"), BOOTS("Boots"),
    ACCESSORY("Accessory"), MAIN_HAND("Main Hand"), OFF_HAND("Off-hand");
}
enum class SkillKind(val label: String) { DAMAGE("Damage"), HEAL("Healing"), BUFF("Buff"), DEBUFF("Debuff"), UTILITY("Utility") }
enum class EntryType(val label: String) {
    QUEST("Quest"), NPC("NPC"), LOCATION("Location"), LORE("Lore"), EVENT("Event"), NOTE("Note"),
    ACHIEVEMENT("Achievement"), REWARD("Reward");
}
enum class EntryStatus(val label: String) { ACTIVE("Active"), COMPLETED("Completed"), FAILED("Failed"), ARCHIVED("Archived") }

@Serializable
@Entity(tableName = "campaign")
data class Campaign(
    @PrimaryKey val id: String,
    val name: String,
    val mode: CampaignMode,
    /** Seeds the roll audit chain genesis (not used to generate dice — dice use SecureRandom). */
    val seed: String,
    val hostDeviceId: String,
    /** True on the host / local device; false for a player's mirror of someone else's campaign. */
    val isHost: Boolean = true,
    val tLow: Int = 20, val tLowMedium: Int = 40, val tMedium: Int = 60, val tHighMedium: Int = 80,
    /** Last audit hash of the roll chain and number of rolls so far. */
    val rollChain: String = "",
    val rollIdx: Int = 0,
    /** Host: last change sequence issued. Client mirror: last host sequence applied. */
    val lastSeq: Long = 0,
    val epoch: Int = 1,
    val weightLimit: Int = 0,
    val rev: Long = 1,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
@Entity(tableName = "character", indices = [Index("campaignId")])
data class GameCharacter(
    @PrimaryKey val id: String,
    val campaignId: String,
    val name: String,
    val art: String = "",
    val level: Int = 1,
    val exp: Long = 0,
    val className: String = "Adventurer",
    val job: String = "",
    val race: String = "Human",
    val background: String = "",
    val description: String = "",
    val baseHp: Int = 30, val baseMp: Int = 10, val baseAtk: Int = 5, val baseDef: Int = 3, val baseSpd: Int = 5,
    val curHp: Int = 30, val curMp: Int = 10,
    val str: Int = 10, val dex: Int = 10, val con: Int = 10, val intel: Int = 10, val wis: Int = 10, val cha: Int = 10,
    val notes: String = "",
    val ownerDeviceId: String? = null,
    val isNpc: Boolean = false,
    val rev: Long = 1,
)

@Serializable
@Entity(tableName = "item", indices = [Index("campaignId", "ownerId"), Index("ownerId")])
data class Item(
    @PrimaryKey val id: String,
    val campaignId: String,
    /** Character id, or null for the shared party stash. */
    val ownerId: String?,
    val name: String,
    val kind: ItemKind = ItemKind.MISC,
    val slot: EquipSlot? = null,
    val rarity: Rarity = Rarity.COMMON,
    val description: String = "",
    val quantity: Int = 1,
    val weight: Int = 0,
    val art: String = "",
    val equipped: Boolean = false,
    val bonusHp: Int = 0, val bonusMp: Int = 0, val bonusAtk: Int = 0, val bonusDef: Int = 0, val bonusSpd: Int = 0,
    /** Consumable effect: restores [healHp]/[healMp] when used. */
    val healHp: Int = 0, val healMp: Int = 0,
    val value: Long = 0,
    val rev: Long = 1,
) {
    val stackable: Boolean get() = slot == null
}

@Serializable
@Entity(tableName = "skill", indices = [Index("characterId")])
data class Skill(
    @PrimaryKey val id: String,
    val characterId: String,
    val name: String,
    val level: Int = 1,
    val description: String = "",
    val rarity: Rarity = Rarity.COMMON,
    val manaCost: Int = 0,
    val cooldown: Int = 0,
    val kind: SkillKind = SkillKind.DAMAGE,
    val power: Int = 0,
    val effect: String = "",
    val art: String = "",
    val rev: Long = 1,
)

@Serializable
@Entity(tableName = "status_effect", indices = [Index("targetId")])
data class StatusEffect(
    @PrimaryKey val id: String,
    val targetId: String,
    val name: String,
    val description: String = "",
    val isBuff: Boolean = false,
    /** Remaining turns; 0 = until removed. */
    val turns: Int = 0,
    val modHp: Int = 0, val modMp: Int = 0, val modAtk: Int = 0, val modDef: Int = 0, val modSpd: Int = 0,
    val rev: Long = 1,
)

@Serializable
@Entity(tableName = "wallet", indices = [Index("campaignId", "ownerId", unique = true)])
data class Wallet(
    /** `party:<campaignId>` or `pc:<characterId>`. */
    @PrimaryKey val id: String,
    val campaignId: String,
    val ownerId: String?,
    val balance: Long = 0,
    /** Hidden wallets are not synced to players. */
    val hidden: Boolean = false,
    val rev: Long = 1,
) {
    companion object {
        fun partyId(campaignId: String) = "party:$campaignId"
        fun personalId(characterId: String) = "pc:$characterId"
    }
}

@Serializable
@Entity(tableName = "money_tx", indices = [Index("campaignId", "ts")])
data class MoneyTx(
    @PrimaryKey val id: String,
    val campaignId: String,
    val ts: Long,
    val fromWallet: String?,
    val toWallet: String?,
    val amount: Long,
    val reason: String = "",
    val actor: String = "DM",
    val audience: String = "*",
)

@Serializable
@Entity(tableName = "entry", indices = [Index("campaignId", "type")])
data class Entry(
    @PrimaryKey val id: String,
    val campaignId: String,
    val type: EntryType,
    val title: String,
    val body: String = "",
    val status: EntryStatus = EntryStatus.ACTIVE,
    /** Character the entry belongs to (personal note, personal quest, achievement) or null for campaign-wide. */
    val ownerId: String? = null,
    val rewardBronze: Long = 0,
    val art: String = "",
    /** Public entries are synced to players; private ones are DM-only. */
    val isPublic: Boolean = false,
    val createdAt: Long = 0,
    val rev: Long = 1,
)

@Serializable
@Entity(tableName = "encounter", indices = [Index("campaignId")])
data class Encounter(
    @PrimaryKey val id: String,
    val campaignId: String,
    val name: String,
    val active: Boolean = false,
    val round: Int = 1,
    val turnIndex: Int = 0,
    val createdAt: Long = 0,
    val rev: Long = 1,
)

@Serializable
@Entity(tableName = "combatant", indices = [Index("encounterId")])
data class Combatant(
    @PrimaryKey val id: String,
    val encounterId: String,
    val campaignId: String,
    val characterId: String? = null,
    val name: String,
    val initiative: Int = 0,
    val hp: Int = 10,
    val maxHp: Int = 10,
    val defense: Int = 0,
    val isPlayer: Boolean = false,
    val art: String = "",
    val notes: String = "",
    val rev: Long = 1,
)

@Serializable
@Entity(tableName = "roll", indices = [Index("campaignId", "idx")])
data class RollRecord(
    @PrimaryKey val id: String,
    val campaignId: String,
    val idx: Int,
    val ts: Long,
    val actor: String = "DM",
    val label: String = "",
    val expr: String,
    val dice: String,
    val modifier: Int = 0,
    val total: Int,
    val tier: String,
    val crit: String = "NONE",
    val isPublic: Boolean = true,
    val auditHash: String = "",
)

@Entity(tableName = "event_log", indices = [Index("campaignId", "seq")])
data class EventLogEntry(
    @PrimaryKey(autoGenerate = true) val seq: Long = 0,
    val campaignId: String,
    val ts: Long,
    val type: String,
    val text: String,
    val actor: String = "DM",
)

/** Compacted change log: exactly one row per entity, restamped with a fresh [seq] on each change. */
@Serializable
@Entity(tableName = "change", primaryKeys = ["campaignId", "entityType", "entityId"], indices = [Index("campaignId", "seq", unique = true)])
data class Change(
    val campaignId: String,
    val entityType: String,
    val entityId: String,
    val seq: Long,
    /** `*` everyone, `DM` host only, or comma separated character ids. */
    val audience: String = "*",
    /** Former audience members that must drop the entity (e.g. an item that changed owner). `*` = everyone else. */
    val goneFor: String = "",
    val payload: String = "",
    val deleted: Boolean = false,
)

@Serializable
@Entity(tableName = "member", primaryKeys = ["campaignId", "deviceId"], indices = [Index("campaignId")])
data class Member(
    val campaignId: String,
    val deviceId: String,
    val characterId: String,
    val name: String,
    val token: String = "",
    val lastSeen: Long = 0,
    val lastSeq: Long = 0,
)

@Serializable
@Entity(tableName = "processed_action", indices = [Index("deviceId", "ts")])
data class ProcessedAction(
    @PrimaryKey val actionId: String,
    val deviceId: String,
    val ts: Long,
    val ok: Boolean,
    val message: String,
    @ColumnInfo(defaultValue = "") val seqAfter: String = "",
)
