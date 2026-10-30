package com.runehearth.app.core

/**
 * Integer-only currency math. The smallest unit is the bronze coin; every balance in the app is a [Long]
 * number of bronze. Conversions: 10 bronze = 1 silver, 10 silver = 1 gold, 20 gold = 1 holy gold.
 */
enum class Coin(val label: String, val short: String, val bronzeValue: Long) {
    BRONZE("Bronze", "B", 1L),
    SILVER("Silver", "S", 10L),
    GOLD("Gold", "G", 100L),
    HOLY_GOLD("Holy Gold", "HG", 2000L);
}

data class CoinBreakdown(val holyGold: Long, val gold: Long, val silver: Long, val bronze: Long) {
    fun amountOf(coin: Coin): Long = when (coin) {
        Coin.BRONZE -> bronze
        Coin.SILVER -> silver
        Coin.GOLD -> gold
        Coin.HOLY_GOLD -> holyGold
    }
}

object Money {
    const val MAX_BALANCE: Long = 9_000_000_000_000L

    /** Greedy breakdown of [totalBronze] into the largest denominations. Negative totals break down their magnitude. */
    fun breakdown(totalBronze: Long): CoinBreakdown {
        var rest = kotlin.math.abs(totalBronze)
        val holy = rest / Coin.HOLY_GOLD.bronzeValue; rest %= Coin.HOLY_GOLD.bronzeValue
        val gold = rest / Coin.GOLD.bronzeValue; rest %= Coin.GOLD.bronzeValue
        val silver = rest / Coin.SILVER.bronzeValue; rest %= Coin.SILVER.bronzeValue
        return CoinBreakdown(holy, gold, silver, rest)
    }

    /** Converts a quantity of one coin to bronze, throwing [ArithmeticException] on overflow. */
    fun toBronze(amount: Long, coin: Coin): Long = Math.multiplyExact(amount, coin.bronzeValue)

    fun of(holyGold: Long = 0, gold: Long = 0, silver: Long = 0, bronze: Long = 0): Long =
        Math.addExact(
            Math.addExact(toBronze(holyGold, Coin.HOLY_GOLD), toBronze(gold, Coin.GOLD)),
            Math.addExact(toBronze(silver, Coin.SILVER), bronze),
        )

    /** Compact human form, e.g. `1 HG 4 G 2 S 5 B`, or `0 B`. */
    fun format(totalBronze: Long): String {
        val b = breakdown(totalBronze)
        val parts = buildList {
            if (b.holyGold > 0) add("${b.holyGold} HG")
            if (b.gold > 0) add("${b.gold} G")
            if (b.silver > 0) add("${b.silver} S")
            if (b.bronze > 0) add("${b.bronze} B")
        }
        val s = if (parts.isEmpty()) "0 B" else parts.joinToString(" ")
        return if (totalBronze < 0) "-$s" else s
    }

    /**
     * Converts [amount] of coin [from] into the maximum whole number of coin [to]; returns the converted count and the
     * bronze remainder that could not be converted.
     */
    fun convert(amount: Long, from: Coin, to: Coin): Pair<Long, Long> {
        val bronze = toBronze(amount, from)
        return (bronze / to.bronzeValue) to (bronze % to.bronzeValue)
    }
}
