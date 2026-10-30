package com.runehearth.app.engine

import androidx.room.withTransaction
import com.runehearth.app.core.AuditChain
import com.runehearth.app.core.DiceEngine
import com.runehearth.app.core.Money
import com.runehearth.app.core.RandomSource
import com.runehearth.app.core.SecureSource
import com.runehearth.app.core.TierThresholds
import com.runehearth.app.data.Campaign
import com.runehearth.app.data.Change
import com.runehearth.app.data.Combatant
import com.runehearth.app.data.EntryStatus
import com.runehearth.app.data.EntryType
import com.runehearth.app.data.EquipSlot
import com.runehearth.app.data.EventLogEntry
import com.runehearth.app.data.GameCharacter
import com.runehearth.app.data.GameDatabase
import com.runehearth.app.data.Item
import com.runehearth.app.data.ItemKind
import com.runehearth.app.data.MoneyTx
import com.runehearth.app.data.ProcessedAction
import com.runehearth.app.data.RollRecord
import com.runehearth.app.data.Wallet
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/** Thrown by validation; rolls back the whole transaction and becomes a failed [ActionResult]. */
class Reject(message: String) : Exception(message)

data class ChangeBatch(val campaignId: String, val changes: List<Change>)

/**
 * The single writer for game state. Executes [Action]s atomically (one DB transaction each), serialised by a mutex,
 * validated against the [Actor]'s permissions, de-duplicated by action id, and records every touched entity in the
 * compacted change log so it can be synced incrementally.
 */
class ActionEngine(
    private val db: GameDatabase,
    private val random: RandomSource = SecureSource(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    private val dao = db.dao()
    private val mutex = Mutex()
    private val _changes = MutableSharedFlow<ChangeBatch>(extraBufferCapacity = 64)

    /** Emits the changes of every successfully applied action (the host forwards them to clients). */
    val changes: SharedFlow<ChangeBatch> = _changes

    suspend fun execute(campaignId: String, actor: Actor, action: Action): ActionResult = mutex.withLock {
        dao.processed(action.id)?.let { return ActionResult(action.id, it.ok, it.message, seq = it.seqAfter.toLongOrNull() ?: 0) }
        val ctx = Ctx(campaignId, actor)
        val result = try {
            val r = db.withTransaction {
                ctx.camp = dao.campaign(campaignId) ?: throw Reject("Campaign not found")
                val partial = ctx.handle(action)
                val seq = ctx.flush()
                partial.copy(seq = seq)
            }
            _changes.tryEmit(ChangeBatch(campaignId, ctx.emitted))
            r
        } catch (e: Reject) {
            ActionResult(action.id, false, e.message ?: "Rejected")
        }
        if (actor.deviceId.isNotEmpty()) {
            dao.upsert(ProcessedAction(action.id, actor.deviceId, clock(), result.ok, result.message, result.seq.toString()))
            dao.pruneProcessed(actor.deviceId, 200)
        }
        result
    }

    /** Rebuilds the change log from scratch (after import / restore). */
    suspend fun rebuildChangeLog(campaignId: String) = mutex.withLock {
        db.withTransaction {
            val camp = dao.campaign(campaignId) ?: return@withTransaction
            dao.clearChanges(campaignId)
            val ctx = Ctx(campaignId, Actor.dm(""))
            ctx.camp = camp
            ctx.mark(EntityType.CAMPAIGN, campaignId)
            val chars = dao.characters(campaignId)
            for (c in chars) {
                ctx.mark(EntityType.CHARACTER, c.id)
                dao.itemsOf(c.id).forEach { ctx.mark(EntityType.ITEM, it.id) }
                dao.skills(c.id).forEach { ctx.mark(EntityType.SKILL, it.id) }
                dao.statuses(c.id).forEach { ctx.mark(EntityType.STATUS, it.id) }
                ctx.mark(EntityType.WALLET, Wallet.personalId(c.id))
            }
            dao.stash(campaignId).forEach { ctx.mark(EntityType.ITEM, it.id) }
            ctx.mark(EntityType.WALLET, Wallet.partyId(campaignId))
            for (e in dao.activeEncounters(campaignId)) {
                ctx.mark(EntityType.ENCOUNTER, e.id)
                dao.combatants(e.id).forEach { ctx.mark(EntityType.COMBATANT, it.id) }
            }
            ctx.flush()
        }
    }

    private inner class Ctx(val cid: String, val actor: Actor) {
        lateinit var camp: Campaign
        private val dirty = LinkedHashSet<Pair<EntityType, String>>()
        val emitted = ArrayList<Change>()

        fun mark(type: EntityType, id: String) { dirty.add(type to id) }
        fun reject(msg: String): Nothing = throw Reject(msg)
        private fun requireDm() { if (!actor.isDm) reject("Only the Dungeon Master can do that") }
        private fun requireSelfOrDm(characterId: String) {
            if (!actor.isDm && actor.characterId != characterId) reject("That is not your character")
        }

        suspend fun log(type: String, text: String) {
            val seq = dao.insert(EventLogEntry(campaignId = cid, ts = clock(), type = type, text = text, actor = actor.tag))
            if (seq % 100 == 0L) dao.pruneEvents(cid, 1000)
        }

        suspend fun flush(): Long {
            var seq = camp.lastSeq
            for ((type, id) in dirty) {
                val snap = EntityStore.snapshot(dao, type, id)
                val prev = dao.change(cid, type.name, id)
                val change = when {
                    snap == null || !snap.visible -> {
                        if (prev == null || prev.deleted) continue
                        Change(cid, type.name, id, ++seq, prev.audience, Aud.union(prev.audience, prev.goneFor), "", true)
                    }
                    else -> {
                        val gone = if (snap.audience == Aud.ALL || prev == null) "" else Aud.union(prev.audience, prev.goneFor)
                        Change(cid, type.name, id, ++seq, snap.audience, gone, snap.payload, false)
                    }
                }
                dao.upsert(change)
                emitted += change
            }
            camp = camp.copy(lastSeq = seq, updatedAt = clock())
            dao.upsert(camp)
            return seq
        }

        // ---------- helpers
        private fun String.clip(max: Int) = if (length > max) substring(0, max) else this
        private fun name(s: String): String = s.trim().clip(60).also { if (it.isEmpty()) reject("A name is required") }

        private suspend fun requireChar(id: String): GameCharacter =
            dao.character(id)?.takeIf { it.campaignId == cid } ?: reject("Character not found")

        private suspend fun statsOf(c: GameCharacter): FinalStats =
            StatCalculator.compute(c, dao.equippedOf(c.id), dao.statuses(c.id))

        /** Writes a character with vitals clamped to current maximums and a bumped rev. */
        private suspend fun saveChar(c: GameCharacter) {
            val s = statsOf(c)
            dao.upsert(c.copy(curHp = c.curHp.coerceIn(0, s.maxHp), curMp = c.curMp.coerceIn(0, s.maxMp), rev = c.rev + 1))
            mark(EntityType.CHARACTER, c.id)
        }

        private suspend fun reclamp(characterId: String) = dao.character(characterId)?.let { saveChar(it) }

        private suspend fun ensureWallet(id: String, owner: String?) {
            if (dao.wallet(id) == null) {
                dao.upsert(Wallet(id, cid, owner)); mark(EntityType.WALLET, id)
            }
        }

        private suspend fun requireWallet(id: String): Wallet =
            dao.wallet(id)?.takeIf { it.campaignId == cid } ?: reject("Wallet not found")

        private suspend fun txAudience(vararg wallets: Wallet?): String {
            val ws = wallets.filterNotNull()
            if (ws.any { it.hidden }) return Aud.DM
            val owners = ws.mapNotNull { it.ownerId }
            return if (owners.isEmpty()) Aud.ALL else Aud.of(*owners.toTypedArray())
        }

        private suspend fun recordTx(from: Wallet?, to: Wallet?, amount: Long, reason: String) {
            val id = newId()
            dao.upsert(MoneyTx(id, cid, clock(), from?.id, to?.id, amount, reason.clip(120), actor.tag, txAudience(from, to)))
            mark(EntityType.TX, id)
            dao.oldTxIds(cid, 1500).forEach { dao.deleteTx(it); mark(EntityType.TX, it) }
        }

        private suspend fun credit(w: Wallet, delta: Long) {
            val nb = w.balance + delta
            if (nb < 0) reject("Insufficient funds (${Money.format(w.balance)})")
            if (nb > Money.MAX_BALANCE) reject("Balance too large")
            dao.upsert(w.copy(balance = nb, rev = w.rev + 1)); mark(EntityType.WALLET, w.id)
        }

        private fun stackKey(i: Item) = listOf(i.name.lowercase(), i.kind, i.rarity, i.art, i.description, i.bonusHp, i.bonusMp, i.bonusAtk, i.bonusDef, i.bonusSpd, i.healHp, i.healMp)

        private suspend fun addToOwner(item: Item, ownerId: String?): Item {
            val existing = (if (ownerId == null) dao.stash(cid) else dao.itemsOf(ownerId))
                .firstOrNull { item.stackable && it.stackable && !it.equipped && stackKey(it) == stackKey(item) }
            return if (existing != null) {
                val merged = existing.copy(quantity = (existing.quantity + item.quantity).coerceAtMost(9999), rev = existing.rev + 1)
                dao.upsert(merged); mark(EntityType.ITEM, merged.id); merged
            } else {
                val placed = item.copy(ownerId = ownerId, equipped = false, rev = 1)
                dao.upsert(placed); mark(EntityType.ITEM, placed.id); placed
            }
        }

        // ---------- dispatch
        suspend fun handle(a: Action): ActionResult {
            when (a) {
                is Action.UpdateCampaign -> {
                    requireDm()
                    val n = a.campaign
                    val t = TierThresholds(n.tLow, n.tLowMedium, n.tMedium, n.tHighMedium)
                    if (!t.isValid()) reject("Roll thresholds must be increasing percentages")
                    camp = camp.copy(name = name(n.name), tLow = n.tLow, tLowMedium = n.tLowMedium, tMedium = n.tMedium,
                        tHighMedium = n.tHighMedium, weightLimit = n.weightLimit.coerceIn(0, 100000), rev = camp.rev + 1)
                    mark(EntityType.CAMPAIGN, cid)
                }
                is Action.UpsertCharacter -> upsertCharacter(a)
                is Action.DeleteCharacter -> {
                    requireDm()
                    val c = requireChar(a.characterId)
                    val personal = dao.wallet(Wallet.personalId(c.id))
                    if (personal != null && personal.balance > 0) {
                        val party = requireWallet(Wallet.partyId(cid))
                        credit(party, personal.balance)
                    }
                    dao.itemsOf(c.id).forEach { mark(EntityType.ITEM, it.id) }
                    dao.skills(c.id).forEach { mark(EntityType.SKILL, it.id) }
                    dao.statuses(c.id).forEach { mark(EntityType.STATUS, it.id) }
                    dao.deleteItemsOf(c.id); dao.deleteSkillsOf(c.id); dao.deleteStatusesOf(c.id)
                    dao.deleteWallet(Wallet.personalId(c.id)); mark(EntityType.WALLET, Wallet.personalId(c.id))
                    dao.deleteCharacter(c.id); mark(EntityType.CHARACTER, c.id)
                    log("character", "${c.name} left the party")
                }
                is Action.AwardExp -> {
                    requireDm()
                    val c = requireChar(a.characterId)
                    if (a.amount !in 0..1_000_000) reject("Invalid experience amount")
                    val (n, gained) = Leveling.award(c, a.amount)
                    saveChar(n.copy(curHp = n.curHp + gained * Leveling.HP_PER_LEVEL, curMp = n.curMp + gained * Leveling.MP_PER_LEVEL))
                    log("exp", "${c.name} gained ${a.amount} XP" + if (gained > 0) " and reached level ${n.level}!" else "")
                }
                is Action.AdjustVitals -> {
                    requireDm()
                    val c = requireChar(a.characterId)
                    val s = statsOf(c)
                    saveChar(c.copy(curHp = (c.curHp + a.dHp).coerceIn(0, s.maxHp), curMp = (c.curMp + a.dMp).coerceIn(0, s.maxMp)))
                    if (a.dHp != 0) log("vitals", "${c.name} ${if (a.dHp < 0) "took ${-a.dHp} damage" else "healed ${a.dHp} HP"}")
                }
                is Action.UpdateNotes -> {
                    requireSelfOrDm(a.characterId)
                    val c = requireChar(a.characterId)
                    if (a.expectedRev >= 0 && a.expectedRev != c.rev) reject("stale: the sheet changed, resynced")
                    saveChar(c.copy(notes = a.notes.clip(8000)))
                }
                is Action.UpsertItem -> upsertItem(a)
                is Action.DeleteItem -> {
                    requireDm()
                    dao.item(a.itemId)?.let { dao.deleteItem(it.id); mark(EntityType.ITEM, it.id); it.ownerId?.let { o -> reclamp(o) } }
                }
                is Action.Equip -> equip(a.itemId)
                is Action.Unequip -> {
                    val i = ownedItem(a.itemId)
                    if (i.equipped) {
                        dao.upsert(i.copy(equipped = false, rev = i.rev + 1)); mark(EntityType.ITEM, i.id)
                        i.ownerId?.let { reclamp(it) }
                    }
                }
                is Action.UseItem -> useItem(a.itemId)
                is Action.TransferItem -> transferItem(a)
                is Action.UpsertSkill -> {
                    requireDm()
                    val s = a.skill
                    requireChar(s.characterId)
                    val old = dao.skill(s.id)
                    dao.upsert(s.copy(name = name(s.name), description = s.description.clip(600), effect = s.effect.clip(300),
                        level = s.level.coerceIn(1, 99), manaCost = s.manaCost.coerceIn(0, 9999), cooldown = s.cooldown.coerceIn(0, 99),
                        rev = (old?.rev ?: 0) + 1))
                    mark(EntityType.SKILL, s.id)
                }
                is Action.DeleteSkill -> {
                    requireDm()
                    dao.deleteSkill(a.skillId); mark(EntityType.SKILL, a.skillId)
                }
                is Action.CastSkill -> {
                    val s = dao.skill(a.skillId) ?: reject("Skill not found")
                    requireSelfOrDm(s.characterId)
                    val c = requireChar(s.characterId)
                    if (c.curMp < s.manaCost) reject("Not enough mana (${c.curMp}/${s.manaCost})")
                    saveChar(c.copy(curMp = c.curMp - s.manaCost))
                    log("cast", "${c.name} cast ${s.name}" + if (a.targetName.isNotBlank()) " on ${a.targetName.clip(40)}" else "")
                }
                is Action.UpsertStatus -> {
                    requireDm()
                    val s = a.status
                    if (dao.character(s.targetId) == null && dao.combatant(s.targetId) == null) reject("Target not found")
                    val old = dao.status(s.id)
                    dao.upsert(s.copy(name = name(s.name), description = s.description.clip(300), turns = s.turns.coerceIn(0, 999), rev = (old?.rev ?: 0) + 1))
                    mark(EntityType.STATUS, s.id)
                    reclamp(s.targetId)
                }
                is Action.DeleteStatus -> {
                    requireDm()
                    dao.status(a.statusId)?.let { dao.deleteStatus(it.id); mark(EntityType.STATUS, it.id); reclamp(it.targetId) }
                }
                is Action.AdjustWallet -> {
                    requireDm()
                    val w = requireWallet(a.walletId)
                    if (a.delta == 0L) reject("Amount must not be zero")
                    credit(w, a.delta)
                    val fresh = dao.wallet(w.id)!!
                    if (a.delta > 0) recordTx(null, fresh, a.delta, a.reason) else recordTx(fresh, null, -a.delta, a.reason)
                    log("economy", "${if (a.delta > 0) "+" else ""}${Money.format(a.delta)} ${walletLabel(w)}" + if (a.reason.isNotBlank()) " (${a.reason.clip(60)})" else "")
                }
                is Action.TransferFunds -> transferFunds(a)
                is Action.SetWalletHidden -> {
                    requireDm()
                    val w = requireWallet(a.walletId)
                    dao.upsert(w.copy(hidden = a.hidden, rev = w.rev + 1)); mark(EntityType.WALLET, w.id)
                }
                is Action.UpsertEntry -> upsertEntry(a)
                is Action.DeleteEntry -> {
                    val e = dao.entry(a.entryId)
                    if (e != null) {
                        if (!actor.isDm && !(e.type == EntryType.NOTE && e.ownerId == actor.characterId)) reject("Only the Dungeon Master can do that")
                        dao.deleteEntry(e.id); mark(EntityType.ENTRY, e.id)
                    }
                }
                is Action.CompleteQuest -> {
                    requireDm()
                    val e = dao.entry(a.entryId) ?: reject("Quest not found")
                    if (e.type != EntryType.QUEST) reject("Not a quest")
                    if (e.status != EntryStatus.ACTIVE) reject("Quest is already ${e.status.label.lowercase()}")
                    dao.upsert(e.copy(status = if (a.success) EntryStatus.COMPLETED else EntryStatus.FAILED, rev = e.rev + 1)); mark(EntityType.ENTRY, e.id)
                    if (a.success && e.rewardBronze > 0) {
                        ensureWallet(Wallet.partyId(cid), null)
                        val party = requireWallet(Wallet.partyId(cid))
                        credit(party, e.rewardBronze)
                        recordTx(null, dao.wallet(party.id), e.rewardBronze, "Quest: ${e.title}")
                    }
                    log("quest", "Quest \"${e.title}\" ${if (a.success) "completed" else "failed"}")
                }
                is Action.UpsertEncounter -> {
                    requireDm()
                    val e = a.encounter
                    val old = dao.encounter(e.id)
                    dao.upsert(e.copy(campaignId = cid, name = name(e.name), active = old?.active ?: false, round = old?.round ?: 1,
                        turnIndex = old?.turnIndex ?: 0, createdAt = old?.createdAt ?: clock(), rev = (old?.rev ?: 0) + 1))
                    mark(EntityType.ENCOUNTER, e.id)
                }
                is Action.DeleteEncounter -> {
                    requireDm()
                    val e = dao.encounter(a.encounterId) ?: return ok(a)
                    dao.combatants(e.id).forEach { dao.deleteStatusesOf(it.id); mark(EntityType.COMBATANT, it.id) }
                    dao.deleteCombatantsOf(e.id)
                    dao.deleteEncounter(e.id); mark(EntityType.ENCOUNTER, e.id)
                }
                is Action.StartEncounter -> {
                    requireDm()
                    val e = dao.encounter(a.encounterId) ?: reject("Encounter not found")
                    for (other in dao.activeEncounters(cid)) if (other.id != e.id) setActive(other.id, false)
                    dao.upsert(e.copy(active = true, round = 1, turnIndex = 0, rev = e.rev + 1)); mark(EntityType.ENCOUNTER, e.id)
                    dao.combatants(e.id).forEach { mark(EntityType.COMBATANT, it.id) }
                    log("combat", "Encounter \"${e.name}\" begins")
                }
                is Action.EndEncounter -> {
                    requireDm()
                    val e = dao.encounter(a.encounterId) ?: reject("Encounter not found")
                    for (c in dao.combatants(e.id)) c.characterId?.let { id ->
                        dao.character(id)?.let { ch -> saveChar(ch.copy(curHp = c.hp)) }
                    }
                    setActive(e.id, false)
                    log("combat", "Encounter \"${e.name}\" ended after ${e.round} round(s)")
                }
                is Action.AddPartyToEncounter -> {
                    requireDm()
                    val e = dao.encounter(a.encounterId) ?: reject("Encounter not found")
                    val present = dao.combatants(e.id).mapNotNull { it.characterId }.toSet()
                    for (c in dao.characters(cid).filter { !it.isNpc && it.id !in present }) {
                        val s = statsOf(c)
                        val cb = Combatant(newId(), e.id, cid, c.id, c.name, 0, c.curHp, s.maxHp, s.def.total, true, c.art)
                        dao.upsert(cb); mark(EntityType.COMBATANT, cb.id)
                    }
                }
                is Action.RollInitiative -> {
                    requireDm()
                    val e = dao.encounter(a.encounterId) ?: reject("Encounter not found")
                    for (c in dao.combatants(e.id)) {
                        val spd = c.characterId?.let { dao.character(it)?.let { ch -> statsOf(ch).spd.total } } ?: 0
                        dao.upsert(c.copy(initiative = random.nextInt(20) + 1 + spd / 2, rev = c.rev + 1)); mark(EntityType.COMBATANT, c.id)
                    }
                    dao.upsert(e.copy(turnIndex = 0, round = 1, rev = e.rev + 1)); mark(EntityType.ENCOUNTER, e.id)
                }
                is Action.NextTurn -> nextTurn(a.encounterId)
                is Action.UpsertCombatant -> {
                    requireDm()
                    val c = a.combatant
                    val enc = dao.encounter(c.encounterId) ?: reject("Encounter not found")
                    val old = dao.combatant(c.id)
                    val max = c.maxHp.coerceIn(1, 99999)
                    dao.upsert(c.copy(campaignId = cid, name = name(c.name), maxHp = max, hp = c.hp.coerceIn(0, max),
                        initiative = c.initiative.coerceIn(-99, 999), rev = (old?.rev ?: 0) + 1))
                    mark(EntityType.COMBATANT, c.id); mark(EntityType.ENCOUNTER, enc.id)
                }
                is Action.DeleteCombatant -> {
                    requireDm()
                    dao.combatant(a.combatantId)?.let { dao.deleteStatusesOf(it.id); dao.deleteCombatant(it.id); mark(EntityType.COMBATANT, it.id) }
                }
                is Action.DamageCombatant -> {
                    requireDm()
                    val c = dao.combatant(a.combatantId) ?: reject("Combatant not found")
                    val hp = (c.hp + a.delta).coerceIn(0, c.maxHp)
                    dao.upsert(c.copy(hp = hp, rev = c.rev + 1)); mark(EntityType.COMBATANT, c.id)
                    c.characterId?.let { id -> dao.character(id)?.let { ch -> saveChar(ch.copy(curHp = hp)) } }
                    log("combat", "${c.name} ${if (a.delta < 0) "takes ${-a.delta} damage" else "heals ${a.delta}"} ($hp/${c.maxHp})")
                }
                is Action.RollDice -> return rollDice(a)
            }
            return ok(a)
        }

        private fun ok(a: Action, msg: String = "") = ActionResult(a.id, true, msg)

        private suspend fun setActive(encId: String, active: Boolean) {
            val e = dao.encounter(encId) ?: return
            dao.upsert(e.copy(active = active, rev = e.rev + 1)); mark(EntityType.ENCOUNTER, e.id)
            dao.combatants(e.id).forEach { mark(EntityType.COMBATANT, it.id) }
        }

        private fun walletLabel(w: Wallet) = if (w.ownerId == null) "to party funds" else "to a personal purse"

        private suspend fun upsertCharacter(a: Action.UpsertCharacter) {
            requireDm()
            val c = a.character
            val old = dao.character(c.id)
            if (old != null && a.expectedRev >= 0 && a.expectedRev != old.rev) reject("stale: the sheet changed, resynced")
            fun st(v: Int) = v.coerceIn(0, 9999)
            val clean = c.copy(
                campaignId = cid, name = name(c.name), className = c.className.clip(40), job = c.job.clip(40), race = c.race.clip(40),
                background = c.background.clip(1000), description = c.description.clip(2000), notes = c.notes.clip(8000),
                level = c.level.coerceIn(1, Leveling.MAX_LEVEL), exp = c.exp.coerceAtLeast(0),
                baseHp = st(c.baseHp).coerceAtLeast(1), baseMp = st(c.baseMp), baseAtk = st(c.baseAtk), baseDef = st(c.baseDef), baseSpd = st(c.baseSpd),
                str = c.str.coerceIn(1, 99), dex = c.dex.coerceIn(1, 99), con = c.con.coerceIn(1, 99),
                intel = c.intel.coerceIn(1, 99), wis = c.wis.coerceIn(1, 99), cha = c.cha.coerceIn(1, 99),
                ownerDeviceId = c.ownerDeviceId ?: old?.ownerDeviceId, rev = old?.rev ?: 0,
            )
            saveChar(clean)
            ensureWallet(Wallet.personalId(c.id), c.id)
            if (old == null) log("character", "${clean.name} joined the party")
        }

        private suspend fun upsertItem(a: Action.UpsertItem) {
            requireDm()
            val i = a.item
            i.ownerId?.let { requireChar(it) }
            val old = dao.item(i.id)
            val clean = i.copy(
                campaignId = cid, name = name(i.name), description = i.description.clip(800),
                quantity = i.quantity.coerceIn(1, 9999), weight = i.weight.coerceIn(0, 9999), value = i.value.coerceIn(0, Money.MAX_BALANCE),
                kind = i.kind, slot = i.slot,
                equipped = (old?.equipped ?: false) && i.slot != null && i.ownerId != null && i.ownerId == old?.ownerId,
                bonusHp = i.bonusHp.coerceIn(-999, 999), bonusMp = i.bonusMp.coerceIn(-999, 999), bonusAtk = i.bonusAtk.coerceIn(-999, 999),
                bonusDef = i.bonusDef.coerceIn(-999, 999), bonusSpd = i.bonusSpd.coerceIn(-999, 999),
                healHp = i.healHp.coerceIn(0, 9999), healMp = i.healMp.coerceIn(0, 9999),
                rev = (old?.rev ?: 0) + 1,
            )
            if (old == null && a.mergeStacks) addToOwner(clean, clean.ownerId) else { dao.upsert(clean); mark(EntityType.ITEM, clean.id) }
            old?.ownerId?.let { reclamp(it) }
            clean.ownerId?.let { reclamp(it) }
        }

        private suspend fun ownedItem(itemId: String): Item {
            val i = dao.item(itemId)?.takeIf { it.campaignId == cid } ?: reject("Item not found")
            if (!actor.isDm && i.ownerId != actor.characterId) reject("That is not your item")
            return i
        }

        private suspend fun equip(itemId: String) {
            val i = ownedItem(itemId)
            val slot = i.slot ?: reject("${i.name} cannot be equipped")
            val owner = i.ownerId ?: reject("Give the item to a character first")
            if (i.equipped) return
            val worn = dao.equippedOf(owner)
            val limit = if (slot == EquipSlot.ACCESSORY) 3 else 1
            val sameSlot = worn.filter { it.slot == slot }
            if (sameSlot.size >= limit) {
                val out = sameSlot.first()
                dao.upsert(out.copy(equipped = false, rev = out.rev + 1)); mark(EntityType.ITEM, out.id)
            }
            // equipping a single item from a stack: split one off
            val one = if (i.quantity > 1) {
                dao.upsert(i.copy(quantity = i.quantity - 1, rev = i.rev + 1)); mark(EntityType.ITEM, i.id)
                i.copy(id = newId(), quantity = 1, rev = 0)
            } else i
            dao.upsert(one.copy(equipped = true, rev = one.rev + 1)); mark(EntityType.ITEM, one.id)
            reclamp(owner)
        }

        private suspend fun useItem(itemId: String) {
            val i = ownedItem(itemId)
            if (i.equipped) reject("Unequip it first")
            if (i.healHp <= 0 && i.healMp <= 0) reject("${i.name} has no usable effect")
            val owner = i.ownerId?.let { requireChar(it) } ?: reject("Give the item to a character first")
            val s = statsOf(owner)
            saveChar(owner.copy(curHp = (owner.curHp + i.healHp).coerceAtMost(s.maxHp), curMp = (owner.curMp + i.healMp).coerceAtMost(s.maxMp)))
            if (i.quantity > 1) dao.upsert(i.copy(quantity = i.quantity - 1, rev = i.rev + 1)) else dao.deleteItem(i.id)
            mark(EntityType.ITEM, i.id)
            log("item", "${owner.name} used ${i.name}")
        }

        private suspend fun transferItem(a: Action.TransferItem) {
            val stashItem = dao.item(a.itemId)?.takeIf { it.campaignId == cid && it.ownerId == null }
            // players may take items from the shared party stash into their own pack
            if (stashItem != null && !actor.isDm && a.toOwnerId != actor.characterId) reject("You can only take stash items for yourself")
            val i = stashItem ?: ownedItem(a.itemId)
            if (i.equipped) reject("Unequip it first")
            val target = a.toOwnerId
            if (target != null) requireChar(target)
            if (target == i.ownerId) reject("Already there")
            val qty = a.quantity.coerceIn(1, i.quantity)
            val from = i.ownerId
            if (qty >= i.quantity) {
                // move whole stack (merge into target stack when possible)
                dao.deleteItem(i.id); mark(EntityType.ITEM, i.id)
                addToOwner(i.copy(quantity = i.quantity), target)
            } else {
                if (!i.stackable) reject("That item cannot be split")
                dao.upsert(i.copy(quantity = i.quantity - qty, rev = i.rev + 1)); mark(EntityType.ITEM, i.id)
                addToOwner(i.copy(id = newId(), quantity = qty), target)
            }
            from?.let { reclamp(it) }
            log("item", "${i.name} x$qty moved")
        }

        private suspend fun transferFunds(a: Action.TransferFunds) {
            if (a.amount <= 0) reject("Amount must be positive")
            if (a.fromWallet == a.toWallet) reject("Choose two different purses")
            val from = requireWallet(a.fromWallet)
            val to = requireWallet(a.toWallet)
            if (!actor.isDm) {
                if (from.ownerId == null || from.ownerId != actor.characterId) reject("You can only spend from your own purse")
                if (to.hidden) reject("Unknown destination")
            }
            credit(from, -a.amount)
            credit(dao.wallet(to.id)!!, a.amount)
            recordTx(dao.wallet(from.id), dao.wallet(to.id), a.amount, a.reason)
            log("economy", "Transfer ${Money.format(a.amount)}")
        }

        private suspend fun upsertEntry(a: Action.UpsertEntry) {
            val e = a.entry
            val old = dao.entry(e.id)
            if (!actor.isDm) {
                val self = actor.characterId ?: reject("Not allowed")
                if (e.type != EntryType.NOTE || e.ownerId != self) reject("Only the Dungeon Master can do that")
                if (old != null && old.ownerId != self) reject("That is not your note")
            }
            if (e.ownerId != null) requireChar(e.ownerId)
            dao.upsert(e.copy(campaignId = cid, title = e.title.trim().clip(100).ifEmpty { reject("A title is required") }, body = e.body.clip(8000),
                rewardBronze = e.rewardBronze.coerceIn(0, Money.MAX_BALANCE), createdAt = old?.createdAt ?: clock(), rev = (old?.rev ?: 0) + 1))
            mark(EntityType.ENTRY, e.id)
        }

        private suspend fun nextTurn(encId: String) {
            requireDm()
            val e = dao.encounter(encId) ?: reject("Encounter not found")
            val list = dao.combatants(e.id)
            if (list.isEmpty()) reject("No combatants")
            val cur = list.getOrNull(e.turnIndex)
            if (cur != null) for (target in listOfNotNull(cur.id, cur.characterId)) {
                for (s in dao.statuses(target)) if (s.turns > 0) {
                    if (s.turns == 1) { dao.deleteStatus(s.id) } else dao.upsert(s.copy(turns = s.turns - 1, rev = s.rev + 1))
                    mark(EntityType.STATUS, s.id)
                }
                reclamp(target)
            }
            var idx = e.turnIndex + 1
            var round = e.round
            if (idx >= list.size) { idx = 0; round++ }
            dao.upsert(e.copy(turnIndex = idx, round = round, rev = e.rev + 1)); mark(EntityType.ENCOUNTER, e.id)
        }

        private suspend fun rollDice(a: Action.RollDice): ActionResult {
            if (!a.spec.isValid()) reject("Invalid dice")
            val th = TierThresholds(camp.tLow, camp.tLowMedium, camp.tMedium, camp.tHighMedium)
            val out = DiceEngine.roll(a.spec, random, th)
            val id = newId()
            val idx = camp.rollIdx + 1
            val prev = camp.rollChain.ifEmpty { AuditChain.genesis(cid, camp.seed) }
            val expr = a.spec.expression()
            val hash = AuditChain.next(prev, id, expr, out.dice)
            dao.upsert(RollRecord(id, cid, idx, clock(), actor.tag, a.label.clip(60), expr, out.dice.joinToString(","), a.spec.modifier,
                out.total, out.tier.name, out.crit.name, a.isPublic, hash))
            mark(EntityType.ROLL, id)
            camp = camp.copy(rollChain = hash, rollIdx = idx)
            dao.oldRollIds(cid, 2000).forEach { dao.deleteRoll(it); mark(EntityType.ROLL, it) }
            return ActionResult(a.id, true, "Rolled ${out.total}", rollId = id)
        }
    }
}
