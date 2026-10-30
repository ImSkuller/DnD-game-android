package com.runehearth.app.data

import com.runehearth.app.core.AuditChain
import com.runehearth.app.core.Money

enum class Severity { ERROR, WARNING }
data class Issue(val severity: Severity, val message: String)

/** Thrown when a bundle fails validation; [issues] lists every problem found. */
class ImportException(val issues: List<Issue>) : Exception(issues.filter { it.severity == Severity.ERROR }.joinToString("; ") { it.message })

/**
 * Validates a campaign bundle before it may become authoritative state (restore from snapshot, host migration, import).
 * Pure function over the bundle so it is fully unit-testable. Errors block the import; warnings are reported.
 */
object BundleValidator {
    fun validate(b: CampaignBundle): List<Issue> {
        val out = ArrayList<Issue>()
        fun err(m: String) { out += Issue(Severity.ERROR, m) }
        fun warn(m: String) { out += Issue(Severity.WARNING, m) }
        if (b.format != 1) err("Unsupported backup format ${b.format}")
        val cid = b.campaign.id
        if (cid.isBlank()) err("The campaign has no id")
        val chars = b.characters.associateBy { it.id }
        val encounters = b.encounters.map { it.id }.toSet()
        val combatants = b.combatants.map { it.id }.toSet()

        if (b.characters.any { it.campaignId != cid }) err("A character belongs to a different campaign")
        if (b.characters.any { it.level !in 1..100 || it.baseHp < 1 || it.curHp < 0 || it.curMp < 0 }) err("A character has impossible stats")
        for (w in b.wallets) {
            if (w.balance < 0 || w.balance > Money.MAX_BALANCE) err("A purse has an impossible balance (${w.balance})")
            if (w.ownerId != null && w.ownerId !in chars) err("A purse belongs to an unknown character")
        }
        if (b.wallets.count { it.ownerId == null } > 1) err("More than one party purse")
        if (b.wallets.none { it.ownerId == null }) warn("The party purse is missing and will be recreated")
        for (i in b.items) {
            if (i.ownerId != null && i.ownerId !in chars) err("Item \"${i.name}\" belongs to an unknown character")
            if (i.quantity < 1) err("Item \"${i.name}\" has an invalid quantity")
            if (i.equipped && (i.slot == null || i.ownerId == null)) err("Item \"${i.name}\" is equipped but cannot be")
        }
        if (b.skills.any { it.characterId !in chars }) err("A skill belongs to an unknown character")
        val statusTargets = chars.keys + combatants
        if (b.statuses.any { it.targetId !in statusTargets }) warn("Some status effects have no target and will be ignored")
        if (b.combatants.any { it.encounterId !in encounters }) err("A combatant belongs to an unknown encounter")
        if (b.transactions.any { it.amount <= 0 }) err("A transaction has a non-positive amount")
        verifyRollChain(b, ::err, ::warn)
        return out
    }

    private fun verifyRollChain(b: CampaignBundle, err: (String) -> Unit, warn: (String) -> Unit) {
        val rolls = b.rolls.sortedBy { it.idx }
        if (rolls.isEmpty()) return
        if (b.campaign.seed.isBlank()) { warn("The roll audit chain cannot be verified (no seed in this copy)"); return }
        val contiguous = rolls.first().idx == 1 && rolls.zipWithNext().all { (a, c) -> c.idx == a.idx + 1 }
        if (!contiguous) { warn("Older rolls were pruned; the audit chain is only partially verifiable"); return }
        var prev = AuditChain.genesis(b.campaign.id, b.campaign.seed)
        for (r in rolls) {
            val dice = r.dice.split(',').mapNotNull { it.toIntOrNull() }
            val expect = AuditChain.next(prev, r.id, r.expr, dice)
            if (expect != r.auditHash) { err("Roll #${r.idx} fails the audit chain: the roll history was altered"); return }
            prev = r.auditHash
        }
        if (b.campaign.rollIdx == rolls.last().idx && b.campaign.rollChain != prev) err("The campaign roll chain head does not match the roll history")
    }
}
