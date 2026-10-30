package com.runehearth.app.engine

import com.runehearth.app.data.Campaign
import com.runehearth.app.data.Change
import com.runehearth.app.data.Combatant
import com.runehearth.app.data.Encounter
import com.runehearth.app.data.Entry
import com.runehearth.app.data.GameCharacter
import com.runehearth.app.data.GameDao
import com.runehearth.app.data.Item
import com.runehearth.app.data.MoneyTx
import com.runehearth.app.data.RollRecord
import com.runehearth.app.data.Skill
import com.runehearth.app.data.StatusEffect
import com.runehearth.app.data.Wallet
import kotlinx.serialization.json.Json
import java.util.zip.CRC32

enum class EntityType { CAMPAIGN, CHARACTER, ITEM, SKILL, STATUS, WALLET, TX, ENTRY, ENCOUNTER, COMBATANT, ROLL }

/** Audience helpers. Audience strings: `*` = everyone, `DM` = host only, otherwise comma separated character ids. */
object Aud {
    const val ALL = "*"
    const val DM = "DM"

    fun tokens(s: String): Set<String> = if (s.isEmpty()) emptySet() else s.split(',').toSet()
    fun contains(aud: String, characterId: String): Boolean = aud == ALL || characterId in tokens(aud)
    fun of(vararg ids: String?): String = ids.filterNotNull().distinct().sorted().joinToString(",").ifEmpty { DM }
    fun union(a: String, b: String): String = when {
        a == ALL || b == ALL -> ALL
        else -> (tokens(a) + tokens(b)).filter { it != DM }.sorted().joinToString(",")
    }
}

/** How a [Change] must be applied for one particular player. */
enum class SyncRoute { UPSERT, DELETE, NONE }

fun Change.routeFor(characterId: String): SyncRoute = when {
    Aud.contains(audience, characterId) -> if (deleted) SyncRoute.DELETE else SyncRoute.UPSERT
    goneFor == Aud.ALL || characterId in Aud.tokens(goneFor) -> SyncRoute.DELETE
    else -> SyncRoute.NONE
}

/** Serialised view of one entity plus who may see it. `visible=false` means "hidden from players" (tombstone). */
data class Snapshot(val payload: String, val audience: String, val visible: Boolean)

/**
 * Maps entity types to DAO reads/writes, serialisation and visibility rules. Used by the host to build the change log
 * and by clients to apply incoming changes to their local mirror.
 */
object EntityStore {
    val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    suspend fun snapshot(dao: GameDao, type: EntityType, id: String): Snapshot? = when (type) {
        EntityType.CAMPAIGN -> dao.campaign(id)?.let { Snapshot(json.encodeToString(Campaign.serializer(), it.forWire()), Aud.ALL, true) }
        EntityType.CHARACTER -> dao.character(id)?.let {
            Snapshot(json.encodeToString(GameCharacter.serializer(), it), it.id, !it.isNpc)
        }
        EntityType.ITEM -> dao.item(id)?.let {
            Snapshot(json.encodeToString(Item.serializer(), it), if (it.ownerId == null) Aud.ALL else it.ownerId, true)
        }
        EntityType.SKILL -> dao.skill(id)?.let { Snapshot(json.encodeToString(Skill.serializer(), it), it.characterId, true) }
        EntityType.STATUS -> dao.status(id)?.let {
            val aud = if (dao.character(it.targetId) != null) it.targetId else Aud.ALL
            Snapshot(json.encodeToString(StatusEffect.serializer(), it), aud, true)
        }
        EntityType.WALLET -> dao.wallet(id)?.let {
            Snapshot(json.encodeToString(Wallet.serializer(), it), it.ownerId ?: Aud.ALL, !it.hidden)
        }
        EntityType.TX -> dao.tx(id)?.let { Snapshot(json.encodeToString(MoneyTx.serializer(), it), it.audience, it.audience != Aud.DM) }
        EntityType.ENTRY -> dao.entry(id)?.let {
            val aud = it.ownerId ?: Aud.ALL
            Snapshot(json.encodeToString(Entry.serializer(), it), aud, it.ownerId != null || it.isPublic)
        }
        EntityType.ENCOUNTER -> dao.encounter(id)?.let {
            Snapshot(json.encodeToString(Encounter.serializer(), it), Aud.ALL, it.active)
        }
        EntityType.COMBATANT -> dao.combatant(id)?.let {
            val active = dao.encounter(it.encounterId)?.active == true
            Snapshot(json.encodeToString(Combatant.serializer(), it), Aud.ALL, active)
        }
        EntityType.ROLL -> dao.roll(id)?.let {
            val aud = if (it.isPublic) Aud.ALL else Aud.of(it.actor.takeIf { a -> a != "DM" })
            Snapshot(json.encodeToString(RollRecord.serializer(), it), aud, it.isPublic || it.actor != "DM")
        }
    }

    /** Host-only fields (roll chain) never leave the host. */
    private fun Campaign.forWire() = copy(rollChain = "", seed = "", hostDeviceId = hostDeviceId)

    /** Applies an incoming change to the local mirror DB (client side). */
    suspend fun apply(dao: GameDao, type: EntityType, id: String, payload: String, delete: Boolean) {
        if (delete) {
            when (type) {
                EntityType.CAMPAIGN -> {}
                EntityType.CHARACTER -> dao.deleteCharacter(id)
                EntityType.ITEM -> dao.deleteItem(id)
                EntityType.SKILL -> dao.deleteSkill(id)
                EntityType.STATUS -> dao.deleteStatus(id)
                EntityType.WALLET -> dao.deleteWallet(id)
                EntityType.TX -> dao.deleteTx(id)
                EntityType.ENTRY -> dao.deleteEntry(id)
                EntityType.ENCOUNTER -> { dao.deleteCombatantsOf(id); dao.deleteEncounter(id) }
                EntityType.COMBATANT -> dao.deleteCombatant(id)
                EntityType.ROLL -> dao.deleteRoll(id)
            }
            return
        }
        when (type) {
            EntityType.CAMPAIGN -> {
                val incoming = json.decodeFromString(Campaign.serializer(), payload)
                val local = dao.campaign(id)
                dao.upsert(incoming.copy(isHost = false, lastSeq = local?.lastSeq ?: 0, mode = incoming.mode))
            }
            EntityType.CHARACTER -> dao.upsert(json.decodeFromString(GameCharacter.serializer(), payload))
            EntityType.ITEM -> dao.upsert(json.decodeFromString(Item.serializer(), payload))
            EntityType.SKILL -> dao.upsert(json.decodeFromString(Skill.serializer(), payload))
            EntityType.STATUS -> dao.upsert(json.decodeFromString(StatusEffect.serializer(), payload))
            EntityType.WALLET -> dao.upsert(json.decodeFromString(Wallet.serializer(), payload))
            EntityType.TX -> dao.upsert(json.decodeFromString(MoneyTx.serializer(), payload))
            EntityType.ENTRY -> dao.upsert(json.decodeFromString(Entry.serializer(), payload))
            EntityType.ENCOUNTER -> dao.upsert(json.decodeFromString(Encounter.serializer(), payload))
            EntityType.COMBATANT -> dao.upsert(json.decodeFromString(Combatant.serializer(), payload))
            EntityType.ROLL -> dao.upsert(json.decodeFromString(RollRecord.serializer(), payload))
        }
    }

    /**
     * Digest over the player's own authoritative data (character, wallet, items, skills, statuses). Computed
     * identically by host (from the authoritative DB) and client (from its mirror); a mismatch triggers repair.
     */
    suspend fun ownedDigest(dao: GameDao, characterId: String): Long {
        val parts = ArrayList<String>()
        dao.character(characterId)?.let { parts += "C" + json.encodeToString(GameCharacter.serializer(), it) }
        dao.wallet(Wallet.personalId(characterId))?.takeIf { !it.hidden }?.let { parts += "W" + json.encodeToString(Wallet.serializer(), it) }
        dao.itemsOf(characterId).sortedBy { it.id }.forEach { parts += "I" + json.encodeToString(Item.serializer(), it) }
        dao.skills(characterId).sortedBy { it.id }.forEach { parts += "S" + json.encodeToString(Skill.serializer(), it) }
        dao.statuses(characterId).sortedBy { it.id }.forEach { parts += "T" + json.encodeToString(StatusEffect.serializer(), it) }
        val crc = CRC32()
        parts.forEach { crc.update(it.toByteArray()) }
        return crc.value
    }
}
