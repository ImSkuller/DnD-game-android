package com.runehearth.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Single DAO for all game tables. Every WHERE/ORDER column used here is covered by an index in [Entities.kt]. */
@Dao
interface GameDao {
    // ---- campaign
    @Query("SELECT * FROM campaign ORDER BY updatedAt DESC")
    fun observeCampaigns(): Flow<List<Campaign>>
    @Query("SELECT * FROM campaign WHERE id = :id")
    fun observeCampaign(id: String): Flow<Campaign?>
    @Query("SELECT * FROM campaign WHERE id = :id")
    suspend fun campaign(id: String): Campaign?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(c: Campaign)
    @Query("DELETE FROM campaign WHERE id = :id")
    suspend fun deleteCampaign(id: String)

    // ---- characters
    @Query("SELECT * FROM character WHERE campaignId = :cid ORDER BY isNpc, name COLLATE NOCASE")
    fun observeCharacters(cid: String): Flow<List<GameCharacter>>
    @Query("SELECT * FROM character WHERE id = :id")
    fun observeCharacter(id: String): Flow<GameCharacter?>
    @Query("SELECT * FROM character WHERE id = :id")
    suspend fun character(id: String): GameCharacter?
    @Query("SELECT * FROM character WHERE campaignId = :cid")
    suspend fun characters(cid: String): List<GameCharacter>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(c: GameCharacter)
    @Query("DELETE FROM character WHERE id = :id")
    suspend fun deleteCharacter(id: String)

    // ---- items
    @Query("SELECT * FROM item WHERE ownerId = :ownerId ORDER BY equipped DESC, kind, name COLLATE NOCASE")
    fun observeItemsOf(ownerId: String): Flow<List<Item>>
    @Query("SELECT * FROM item WHERE campaignId = :cid AND ownerId IS NULL ORDER BY kind, name COLLATE NOCASE")
    fun observeStash(cid: String): Flow<List<Item>>
    @Query("SELECT * FROM item WHERE id = :id")
    suspend fun item(id: String): Item?
    @Query("SELECT * FROM item WHERE ownerId = :ownerId")
    suspend fun itemsOf(ownerId: String): List<Item>
    @Query("SELECT * FROM item WHERE campaignId = :cid AND ownerId IS NULL")
    suspend fun stash(cid: String): List<Item>
    @Query("SELECT * FROM item WHERE ownerId = :ownerId AND equipped = 1")
    suspend fun equippedOf(ownerId: String): List<Item>
    @Query("SELECT * FROM item WHERE ownerId = :ownerId AND equipped = 1")
    fun observeEquippedOf(ownerId: String): Flow<List<Item>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(i: Item)
    @Query("DELETE FROM item WHERE id = :id")
    suspend fun deleteItem(id: String)
    @Query("DELETE FROM item WHERE ownerId = :ownerId")
    suspend fun deleteItemsOf(ownerId: String)

    // ---- skills
    @Query("SELECT * FROM skill WHERE characterId = :cid ORDER BY level DESC, name COLLATE NOCASE")
    fun observeSkills(cid: String): Flow<List<Skill>>
    @Query("SELECT * FROM skill WHERE id = :id")
    suspend fun skill(id: String): Skill?
    @Query("SELECT * FROM skill WHERE characterId = :cid")
    suspend fun skills(cid: String): List<Skill>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(s: Skill)
    @Query("DELETE FROM skill WHERE id = :id")
    suspend fun deleteSkill(id: String)
    @Query("DELETE FROM skill WHERE characterId = :cid")
    suspend fun deleteSkillsOf(cid: String)

    // ---- status effects
    @Query("SELECT * FROM status_effect WHERE targetId = :targetId ORDER BY isBuff DESC, name COLLATE NOCASE")
    fun observeStatuses(targetId: String): Flow<List<StatusEffect>>
    @Query("SELECT * FROM status_effect WHERE targetId = :targetId")
    suspend fun statuses(targetId: String): List<StatusEffect>
    @Query("SELECT * FROM status_effect WHERE id = :id")
    suspend fun status(id: String): StatusEffect?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(s: StatusEffect)
    @Query("DELETE FROM status_effect WHERE id = :id")
    suspend fun deleteStatus(id: String)
    @Query("DELETE FROM status_effect WHERE targetId = :targetId")
    suspend fun deleteStatusesOf(targetId: String)

    // ---- wallets / transactions
    @Query("SELECT * FROM wallet WHERE campaignId = :cid")
    fun observeWallets(cid: String): Flow<List<Wallet>>
    @Query("SELECT * FROM wallet WHERE id = :id")
    suspend fun wallet(id: String): Wallet?
    @Query("SELECT * FROM wallet WHERE id = :id")
    fun observeWallet(id: String): Flow<Wallet?>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(w: Wallet)
    @Query("DELETE FROM wallet WHERE id = :id")
    suspend fun deleteWallet(id: String)
    @Query("SELECT * FROM money_tx WHERE campaignId = :cid ORDER BY ts DESC LIMIT :limit")
    fun observeTx(cid: String, limit: Int): Flow<List<MoneyTx>>
    @Query("SELECT * FROM money_tx WHERE id = :id")
    suspend fun tx(id: String): MoneyTx?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(t: MoneyTx)
    @Query("DELETE FROM money_tx WHERE id = :id")
    suspend fun deleteTx(id: String)
    @Query("SELECT id FROM money_tx WHERE campaignId = :cid ORDER BY ts DESC LIMIT -1 OFFSET :keep")
    suspend fun oldTxIds(cid: String, keep: Int): List<String>

    // ---- entries
    @Query("SELECT * FROM entry WHERE campaignId = :cid AND type = :type ORDER BY status, createdAt DESC")
    fun observeEntries(cid: String, type: EntryType): Flow<List<Entry>>
    @Query("SELECT * FROM entry WHERE campaignId = :cid ORDER BY createdAt DESC")
    fun observeAllEntries(cid: String): Flow<List<Entry>>
    @Query("SELECT * FROM entry WHERE id = :id")
    suspend fun entry(id: String): Entry?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: Entry)
    @Query("DELETE FROM entry WHERE id = :id")
    suspend fun deleteEntry(id: String)

    // ---- combat
    @Query("SELECT * FROM encounter WHERE campaignId = :cid ORDER BY active DESC, createdAt DESC")
    fun observeEncounters(cid: String): Flow<List<Encounter>>
    @Query("SELECT * FROM encounter WHERE id = :id")
    suspend fun encounter(id: String): Encounter?
    @Query("SELECT * FROM encounter WHERE campaignId = :cid AND active = 1")
    suspend fun activeEncounters(cid: String): List<Encounter>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: Encounter)
    @Query("DELETE FROM encounter WHERE id = :id")
    suspend fun deleteEncounter(id: String)
    @Query("SELECT * FROM combatant WHERE encounterId = :eid ORDER BY initiative DESC, name COLLATE NOCASE")
    fun observeCombatants(eid: String): Flow<List<Combatant>>
    @Query("SELECT * FROM combatant WHERE encounterId = :eid ORDER BY initiative DESC, name COLLATE NOCASE")
    suspend fun combatants(eid: String): List<Combatant>
    @Query("SELECT * FROM combatant WHERE id = :id")
    suspend fun combatant(id: String): Combatant?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(c: Combatant)
    @Query("DELETE FROM combatant WHERE id = :id")
    suspend fun deleteCombatant(id: String)
    @Query("DELETE FROM combatant WHERE encounterId = :eid")
    suspend fun deleteCombatantsOf(eid: String)

    // ---- rolls / events
    @Query("SELECT * FROM roll WHERE campaignId = :cid ORDER BY idx DESC LIMIT :limit")
    fun observeRolls(cid: String, limit: Int): Flow<List<RollRecord>>
    @Query("SELECT * FROM roll WHERE id = :id")
    suspend fun roll(id: String): RollRecord?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(r: RollRecord)
    @Query("DELETE FROM roll WHERE id = :id")
    suspend fun deleteRoll(id: String)
    @Query("SELECT id FROM roll WHERE campaignId = :cid ORDER BY idx DESC LIMIT -1 OFFSET :keep")
    suspend fun oldRollIds(cid: String, keep: Int): List<String>

    @Insert
    suspend fun insert(e: EventLogEntry): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvent(e: EventLogEntry)
    @Query("SELECT * FROM event_log WHERE seq = :seq")
    suspend fun event(seq: Long): EventLogEntry?
    @Query("DELETE FROM event_log WHERE seq = :seq")
    suspend fun deleteEvent(seq: Long)
    @Query("SELECT * FROM event_log WHERE campaignId = :cid ORDER BY seq DESC LIMIT 500")
    suspend fun recentEvents(cid: String): List<EventLogEntry>
    @Query("SELECT * FROM event_log WHERE campaignId = :cid ORDER BY seq DESC LIMIT :limit")
    fun observeEvents(cid: String, limit: Int): Flow<List<EventLogEntry>>
    @Query("DELETE FROM event_log WHERE campaignId = :cid AND seq IN (SELECT seq FROM event_log WHERE campaignId = :cid ORDER BY seq DESC LIMIT -1 OFFSET :keep)")
    suspend fun pruneEvents(cid: String, keep: Int)

    // ---- bulk reads for backup / wipe
    @Query("SELECT * FROM encounter WHERE campaignId = :cid")
    suspend fun allEncounters(cid: String): List<Encounter>
    @Query("SELECT * FROM item WHERE campaignId = :cid")
    suspend fun allItems(cid: String): List<Item>
    @Query("SELECT * FROM wallet WHERE campaignId = :cid")
    suspend fun allWallets(cid: String): List<Wallet>
    @Query("SELECT * FROM money_tx WHERE campaignId = :cid")
    suspend fun allTx(cid: String): List<MoneyTx>
    @Query("SELECT * FROM entry WHERE campaignId = :cid")
    suspend fun allEntries(cid: String): List<Entry>
    @Query("SELECT * FROM roll WHERE campaignId = :cid")
    suspend fun allRolls(cid: String): List<RollRecord>

    @Query("SELECT * FROM processed_action ORDER BY ts DESC LIMIT 2000")
    suspend fun recentProcessed(): List<ProcessedAction>

    // ---- change log / members / processed actions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(c: Change)
    @Query("SELECT * FROM change WHERE campaignId = :cid AND seq > :seq ORDER BY seq LIMIT :limit")
    suspend fun changesSince(cid: String, seq: Long, limit: Int = 100_000): List<Change>
    @Query("SELECT * FROM change WHERE campaignId = :cid AND entityType = :type AND entityId = :id")
    suspend fun change(cid: String, type: String, id: String): Change?
    @Query("DELETE FROM change WHERE campaignId = :cid AND deleted = 1 AND seq < :belowSeq")
    suspend fun pruneTombstones(cid: String, belowSeq: Long)
    @Query("DELETE FROM change WHERE campaignId = :cid")
    suspend fun clearChanges(cid: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(m: Member)
    @Query("SELECT * FROM member WHERE campaignId = :cid")
    suspend fun members(cid: String): List<Member>
    @Query("SELECT * FROM member WHERE campaignId = :cid")
    fun observeMembers(cid: String): Flow<List<Member>>
    @Query("SELECT * FROM member WHERE campaignId = :cid AND deviceId = :deviceId")
    suspend fun member(cid: String, deviceId: String): Member?

    @Query("SELECT * FROM processed_action WHERE actionId = :id")
    suspend fun processed(id: String): ProcessedAction?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(p: ProcessedAction)
    @Query("DELETE FROM processed_action WHERE deviceId = :deviceId AND actionId IN (SELECT actionId FROM processed_action WHERE deviceId = :deviceId ORDER BY ts DESC LIMIT -1 OFFSET :keep)")
    suspend fun pruneProcessed(deviceId: String, keep: Int)
}
