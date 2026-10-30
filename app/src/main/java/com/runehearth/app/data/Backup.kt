package com.runehearth.app.data

import androidx.room.withTransaction
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/** Versioned, self-contained export of one campaign (`.rhcamp` file = this JSON). */
@Serializable
data class CampaignBundle(
    val format: Int = 1,
    val campaign: Campaign,
    val characters: List<GameCharacter> = emptyList(),
    val items: List<Item> = emptyList(),
    val skills: List<Skill> = emptyList(),
    val statuses: List<StatusEffect> = emptyList(),
    val wallets: List<Wallet> = emptyList(),
    val transactions: List<MoneyTx> = emptyList(),
    val entries: List<Entry> = emptyList(),
    val encounters: List<Encounter> = emptyList(),
    val combatants: List<Combatant> = emptyList(),
    val rolls: List<RollRecord> = emptyList(),
)

/** Export / import / local snapshots. Import replaces a campaign with the same id in a single transaction. */
class Backup(private val db: GameDatabase) {
    private val dao = db.dao()
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    suspend fun export(campaignId: String): String {
        val camp = dao.campaign(campaignId) ?: error("Campaign not found")
        val chars = dao.characters(campaignId)
        val encs = dao.allEncounters(campaignId)
        val bundle = CampaignBundle(
            campaign = camp,
            characters = chars,
            items = dao.allItems(campaignId),
            skills = chars.flatMap { dao.skills(it.id) },
            statuses = (chars.map { it.id } + encs.flatMap { e -> dao.combatants(e.id).map { it.id } }).flatMap { dao.statuses(it) },
            wallets = dao.allWallets(campaignId),
            transactions = dao.allTx(campaignId),
            entries = dao.allEntries(campaignId),
            encounters = encs,
            combatants = encs.flatMap { dao.combatants(it.id) },
            rolls = dao.allRolls(campaignId),
        )
        return json.encodeToString(CampaignBundle.serializer(), bundle)
    }

    /** Returns the imported campaign id. Throws on malformed input without modifying existing data. */
    suspend fun import(text: String): String {
        val b = json.decodeFromString(CampaignBundle.serializer(), text)
        require(b.format == 1) { "Unsupported backup format ${b.format}" }
        val cid = b.campaign.id
        db.withTransaction {
            wipe(cid)
            dao.upsert(b.campaign)
            b.characters.forEach { dao.upsert(it) }
            b.items.forEach { dao.upsert(it) }
            b.skills.forEach { dao.upsert(it) }
            b.statuses.forEach { dao.upsert(it) }
            b.wallets.forEach { dao.upsert(it) }
            b.transactions.forEach { dao.upsert(it) }
            b.entries.forEach { dao.upsert(it) }
            b.encounters.forEach { dao.upsert(it) }
            b.combatants.forEach { dao.upsert(it) }
            b.rolls.forEach { dao.upsert(it) }
        }
        return cid
    }

    /** Deletes a campaign and everything it owns. */
    suspend fun wipe(cid: String) {
        db.withTransaction {
            for (c in dao.characters(cid)) {
                dao.deleteItemsOf(c.id); dao.deleteSkillsOf(c.id); dao.deleteStatusesOf(c.id); dao.deleteWallet(Wallet.personalId(c.id)); dao.deleteCharacter(c.id)
            }
            for (e in dao.allEncounters(cid)) {
                dao.combatants(e.id).forEach { dao.deleteStatusesOf(it.id) }
                dao.deleteCombatantsOf(e.id); dao.deleteEncounter(e.id)
            }
            dao.allItems(cid).forEach { dao.deleteItem(it.id) }
            dao.allWallets(cid).forEach { dao.deleteWallet(it.id) }
            dao.allTx(cid).forEach { dao.deleteTx(it.id) }
            dao.allEntries(cid).forEach { dao.deleteEntry(it.id) }
            dao.allRolls(cid).forEach { dao.deleteRoll(it.id) }
            dao.pruneEvents(cid, 0)
            dao.clearChanges(cid)
            dao.deleteCampaign(cid)
        }
    }

    /** Writes the fast-readable local snapshot file (`snapshots/<id>.json`), rotating one previous copy. */
    suspend fun snapshot(dir: File, campaignId: String) {
        val text = export(campaignId)
        dir.mkdirs()
        val f = File(dir, "$campaignId.json")
        val prev = File(dir, "$campaignId.prev.json")
        if (f.exists()) { prev.delete(); f.renameTo(prev) }
        val tmp = File(dir, "$campaignId.tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(f)) { f.writeText(text); tmp.delete() } // atomic replace; fall back to direct write
    }

    fun newId(): String = UUID.randomUUID().toString()
}
