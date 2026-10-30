package com.runehearth.app.engine

import com.runehearth.app.core.RollSpec
import com.runehearth.app.data.Campaign
import com.runehearth.app.data.Combatant
import com.runehearth.app.data.Encounter
import com.runehearth.app.data.Entry
import com.runehearth.app.data.GameCharacter
import com.runehearth.app.data.Item
import com.runehearth.app.data.Skill
import com.runehearth.app.data.StatusEffect
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Every state mutation in the app is an [Action] executed by the [ActionEngine] (locally for the DM / local-only
 * mode, on the host for remote players). [id] is a UUID used for duplicate prevention.
 *
 * Players may only submit the actions marked *player*; the host rejects everything else.
 */
@Serializable
sealed interface Action {
    val id: String

    // ---- campaign / characters (DM)
    @Serializable @SerialName("campaign") data class UpdateCampaign(override val id: String, val campaign: Campaign) : Action
    @Serializable @SerialName("pc.up") data class UpsertCharacter(override val id: String, val character: GameCharacter, val expectedRev: Long = -1) : Action
    @Serializable @SerialName("pc.del") data class DeleteCharacter(override val id: String, val characterId: String) : Action
    @Serializable @SerialName("exp") data class AwardExp(override val id: String, val characterId: String, val amount: Long) : Action
    @Serializable @SerialName("vit") data class AdjustVitals(override val id: String, val characterId: String, val dHp: Int, val dMp: Int) : Action
    /** player (own character) */
    @Serializable @SerialName("notes") data class UpdateNotes(override val id: String, val characterId: String, val notes: String, val expectedRev: Long = -1) : Action

    // ---- items / skills / statuses
    @Serializable @SerialName("item.up") data class UpsertItem(override val id: String, val item: Item, val mergeStacks: Boolean = false) : Action
    @Serializable @SerialName("item.del") data class DeleteItem(override val id: String, val itemId: String) : Action
    /** player (own items) */
    @Serializable @SerialName("equip") data class Equip(override val id: String, val itemId: String) : Action
    /** player (own items) */
    @Serializable @SerialName("unequip") data class Unequip(override val id: String, val itemId: String) : Action
    /** player (own items) */
    @Serializable @SerialName("use") data class UseItem(override val id: String, val itemId: String) : Action
    /** player (own items). [toOwnerId] null = party stash. */
    @Serializable @SerialName("give") data class TransferItem(override val id: String, val itemId: String, val toOwnerId: String?, val quantity: Int = 1) : Action
    @Serializable @SerialName("skill.up") data class UpsertSkill(override val id: String, val skill: Skill) : Action
    @Serializable @SerialName("skill.del") data class DeleteSkill(override val id: String, val skillId: String) : Action
    /** player (own skills) */
    @Serializable @SerialName("cast") data class CastSkill(override val id: String, val skillId: String, val targetName: String = "") : Action
    @Serializable @SerialName("status.up") data class UpsertStatus(override val id: String, val status: StatusEffect) : Action
    @Serializable @SerialName("status.del") data class DeleteStatus(override val id: String, val statusId: String) : Action

    // ---- economy
    @Serializable @SerialName("wallet.adj") data class AdjustWallet(override val id: String, val walletId: String, val delta: Long, val reason: String = "") : Action
    /** player (from own personal wallet to party/another player) */
    @Serializable @SerialName("wallet.xfer") data class TransferFunds(override val id: String, val fromWallet: String, val toWallet: String, val amount: Long, val reason: String = "") : Action
    @Serializable @SerialName("wallet.hide") data class SetWalletHidden(override val id: String, val walletId: String, val hidden: Boolean) : Action

    // ---- entries (quests, npcs, ...)
    /** players may only upsert their own private NOTE entries */
    @Serializable @SerialName("entry.up") data class UpsertEntry(override val id: String, val entry: Entry) : Action
    @Serializable @SerialName("entry.del") data class DeleteEntry(override val id: String, val entryId: String) : Action
    @Serializable @SerialName("quest.done") data class CompleteQuest(override val id: String, val entryId: String, val success: Boolean = true) : Action

    // ---- combat
    @Serializable @SerialName("enc.up") data class UpsertEncounter(override val id: String, val encounter: Encounter) : Action
    @Serializable @SerialName("enc.del") data class DeleteEncounter(override val id: String, val encounterId: String) : Action
    @Serializable @SerialName("enc.start") data class StartEncounter(override val id: String, val encounterId: String) : Action
    @Serializable @SerialName("enc.end") data class EndEncounter(override val id: String, val encounterId: String) : Action
    @Serializable @SerialName("enc.party") data class AddPartyToEncounter(override val id: String, val encounterId: String) : Action
    @Serializable @SerialName("enc.init") data class RollInitiative(override val id: String, val encounterId: String) : Action
    @Serializable @SerialName("enc.next") data class NextTurn(override val id: String, val encounterId: String) : Action
    @Serializable @SerialName("cmb.up") data class UpsertCombatant(override val id: String, val combatant: Combatant) : Action
    @Serializable @SerialName("cmb.del") data class DeleteCombatant(override val id: String, val combatantId: String) : Action
    @Serializable @SerialName("cmb.dmg") data class DamageCombatant(override val id: String, val combatantId: String, val delta: Int) : Action

    // ---- dice
    /** player or DM. The host performs the roll. */
    @Serializable @SerialName("roll") data class RollDice(override val id: String, val spec: RollSpec, val label: String = "", val isPublic: Boolean = true) : Action
}

/** Who is submitting an action. [characterId] is null for the DM. */
data class Actor(val isDm: Boolean, val characterId: String?, val deviceId: String) {
    val tag: String get() = if (isDm) "DM" else characterId ?: "?"

    companion object {
        fun dm(deviceId: String) = Actor(true, null, deviceId)
    }
}

@Serializable
data class ActionResult(
    val actionId: String,
    val ok: Boolean,
    val message: String = "",
    /** Roll record id when the action was a dice roll. */
    val rollId: String? = null,
    /** Highest change sequence after applying the action. */
    val seq: Long = 0,
)
