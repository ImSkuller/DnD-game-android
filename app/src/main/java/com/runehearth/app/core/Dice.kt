package com.runehearth.app.core

import kotlinx.serialization.Serializable
import java.security.MessageDigest
import java.security.SecureRandom

/** Source of uniformly distributed integers. Injectable so tests can check distributions and edge cases. */
fun interface RandomSource {
    /** Uniform integer in `[0, bound)`. */
    fun nextInt(bound: Int): Int
}

/**
 * Cryptographically secure source with explicit rejection sampling, so every value in `[0, bound)` has *exactly*
 * the same probability (no modulo bias). Draws unsigned 32-bit words from [SecureRandom] and rejects the final
 * partial bucket.
 */
class SecureSource(private val rng: SecureRandom = SecureRandom()) : RandomSource {
    override fun nextInt(bound: Int): Int {
        require(bound in 1..Int.MAX_VALUE) { "bound must be positive" }
        if (bound == 1) return 0
        val range = 1L shl 32
        val limit = range - (range % bound) // largest multiple of bound <= 2^32
        while (true) {
            val draw = rng.nextInt().toLong() and 0xFFFFFFFFL
            if (draw < limit) return (draw % bound).toInt()
        }
    }
}

enum class RollMode { NORMAL, ADVANTAGE, DISADVANTAGE }

enum class RollTier(val label: String, val flavor: String) {
    LOW("Low Roll", "Fortune turns her face away…"),
    LOW_MEDIUM("Low-Medium Roll", "A feeble showing; the odds are stacked."),
    MEDIUM("Medium Roll", "A steady, unremarkable fate."),
    HIGH_MEDIUM("High-Medium Roll", "Fortune smiles, if only a little."),
    HIGH("High Roll", "The gods themselves cheer!");
}

enum class Crit { NONE, SUCCESS, FAILURE }

/** Percent thresholds (of the achievable range) that separate the five tiers. Configurable per campaign. */
@Serializable
data class TierThresholds(val low: Int = 20, val lowMedium: Int = 40, val medium: Int = 60, val highMedium: Int = 80) {
    fun isValid() = low in 1..99 && low < lowMedium && lowMedium < medium && medium < highMedium && highMedium < 100
}

@Serializable
data class RollSpec(
    val count: Int = 1,
    val sides: Int = 20,
    val modifier: Int = 0,
    val mode: RollMode = RollMode.NORMAL,
    val coin: Boolean = false,
) {
    fun isValid() = coin || (count in 1..MAX_COUNT && sides in 2..MAX_SIDES && modifier in -9999..9999)

    /** Advantage/disadvantage only applies to a single die. */
    val effectiveMode: RollMode get() = if (!coin && count == 1) mode else RollMode.NORMAL

    fun expression(): String = when {
        coin -> "Coin toss"
        else -> buildString {
            append(count).append('d').append(sides)
            if (modifier > 0) append('+').append(modifier) else if (modifier < 0) append(modifier)
            when (effectiveMode) {
                RollMode.ADVANTAGE -> append(" (adv)")
                RollMode.DISADVANTAGE -> append(" (dis)")
                RollMode.NORMAL -> {}
            }
        }
    }

    companion object {
        const val MAX_COUNT = 50
        const val MAX_SIDES = 1000
        val COIN = RollSpec(coin = true)
    }
}

data class RollOutcome(
    val spec: RollSpec,
    /** Every die physically rolled (2 values for advantage/disadvantage). */
    val dice: List<Int>,
    /** The dice that count towards the total. */
    val kept: List<Int>,
    val total: Int,
    val minPossible: Int,
    val maxPossible: Int,
    val tier: RollTier,
    val crit: Crit,
    /** For coin tosses: true = heads. */
    val heads: Boolean? = null,
)

object DiceEngine {
    fun roll(spec: RollSpec, source: RandomSource, thresholds: TierThresholds = TierThresholds()): RollOutcome {
        require(spec.isValid()) { "invalid roll spec" }
        if (spec.coin) {
            val heads = source.nextInt(2) == 0
            return RollOutcome(spec, listOf(if (heads) 1 else 2), listOf(if (heads) 1 else 2), if (heads) 1 else 0, 0, 1,
                if (heads) RollTier.HIGH else RollTier.LOW, Crit.NONE, heads)
        }
        val mode = spec.effectiveMode
        val dice = ArrayList<Int>(if (mode == RollMode.NORMAL) spec.count else 2)
        val rolls = if (mode == RollMode.NORMAL) spec.count else 2
        repeat(rolls) { dice.add(source.nextInt(spec.sides) + 1) }
        val kept = when (mode) {
            RollMode.NORMAL -> dice
            RollMode.ADVANTAGE -> listOf(dice.max())
            RollMode.DISADVANTAGE -> listOf(dice.min())
        }
        val total = kept.sum() + spec.modifier
        val keptCount = kept.size
        val minP = keptCount + spec.modifier
        val maxP = keptCount * spec.sides + spec.modifier
        val crit = if (keptCount == 1 && spec.sides >= 4) when (kept[0]) {
            spec.sides -> Crit.SUCCESS
            1 -> Crit.FAILURE
            else -> Crit.NONE
        } else Crit.NONE
        return RollOutcome(spec, dice, kept, total, minP, maxP, classify(total, minP, maxP, thresholds), crit)
    }

    fun classify(total: Int, min: Int, max: Int, t: TierThresholds): RollTier {
        if (max <= min) return RollTier.MEDIUM
        val pct = ((total - min).toDouble() / (max - min)) * 100.0
        return when {
            pct < t.low -> RollTier.LOW
            pct < t.lowMedium -> RollTier.LOW_MEDIUM
            pct < t.medium -> RollTier.MEDIUM
            pct < t.highMedium -> RollTier.HIGH_MEDIUM
            else -> RollTier.HIGH
        }
    }
}

/** Tamper-evident roll audit chain: `hash = SHA-256(prev ‖ id ‖ expr ‖ results)`, hex encoded. */
object AuditChain {
    fun next(prevHash: String, rollId: String, expr: String, results: List<Int>): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(prevHash.toByteArray())
        md.update(rollId.toByteArray())
        md.update(expr.toByteArray())
        md.update(results.joinToString(",").toByteArray())
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    fun genesis(campaignId: String, seed: String): String = next("genesis", campaignId, seed, emptyList())
}
