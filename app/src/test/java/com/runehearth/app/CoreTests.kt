package com.runehearth.app

import com.runehearth.app.core.AuditChain
import com.runehearth.app.core.Coin
import com.runehearth.app.core.Crit
import com.runehearth.app.core.DiceEngine
import com.runehearth.app.core.Money
import com.runehearth.app.core.RandomSource
import com.runehearth.app.core.RollMode
import com.runehearth.app.core.RollSpec
import com.runehearth.app.core.RollTier
import com.runehearth.app.core.SecureSource
import com.runehearth.app.core.TierThresholds
import com.runehearth.app.data.GameCharacter
import com.runehearth.app.data.Item
import com.runehearth.app.data.StatusEffect
import com.runehearth.app.engine.Aud
import com.runehearth.app.engine.Leveling
import com.runehearth.app.engine.StatCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {
    @Test fun conversionRates() {
        assertEquals(10L, Money.toBronze(1, Coin.SILVER))
        assertEquals(100L, Money.toBronze(1, Coin.GOLD))
        assertEquals(2000L, Money.toBronze(1, Coin.HOLY_GOLD))
        assertEquals(20L * 10 * 10, Money.toBronze(1, Coin.HOLY_GOLD))
    }

    @Test fun breakdownRoundTrips() {
        val total = Money.of(holyGold = 3, gold = 19, silver = 9, bronze = 9)
        val b = Money.breakdown(total)
        assertEquals(3, b.holyGold); assertEquals(19, b.gold); assertEquals(9, b.silver); assertEquals(9, b.bronze)
        assertEquals(total, Money.of(b.holyGold, b.gold, b.silver, b.bronze))
    }

    @Test fun formatAndConvert() {
        assertEquals("0 B", Money.format(0))
        assertEquals("1 HG 1 G 1 S 1 B", Money.format(2111))
        assertEquals(1L to 5L, Money.convert(15, Coin.BRONZE, Coin.SILVER))
        assertEquals(2L to 0L, Money.convert(40, Coin.GOLD, Coin.HOLY_GOLD).let { it.first / 1 to it.second })
    }

    @Test(expected = ArithmeticException::class) fun overflowThrows() { Money.toBronze(Long.MAX_VALUE, Coin.HOLY_GOLD) }
}

class DiceTest {
    @Test fun uniformDistributionChiSquare() {
        val src = SecureSource()
        for (sides in listOf(6, 10, 20, 50, 100)) {
            val n = sides * 4000
            val counts = IntArray(sides)
            repeat(n) { counts[src.nextInt(sides)]++ }
            val expected = n.toDouble() / sides
            val chi = counts.sumOf { (it - expected) * (it - expected) / expected }
            // critical value for p=0.0001 with df=sides-1 is well below df + 6*sqrt(2df) + 20
            val limit = (sides - 1) + 6 * Math.sqrt(2.0 * (sides - 1)) + 20
            assertTrue("d$sides chi-square $chi >= $limit", chi < limit)
        }
    }

    @Test fun boundedSamplingNeverOutOfRangeAndHandlesAwkwardBounds() {
        val src = SecureSource()
        for (bound in listOf(1, 2, 3, 7, 1000, 1_500_000_000, Int.MAX_VALUE)) repeat(500) {
            val v = src.nextInt(bound)
            assertTrue(v in 0 until bound)
        }
    }

    @Test fun rejectionSamplingRemovesModuloBias() {
        // A "source" of 3-bit words would be biased with modulo 3 (0..7 -> 0,1,2,0,1,2,0,1); our engine takes a RandomSource
        // so we verify the engine maps a perfectly uniform source onto equal buckets.
        var i = 0
        val cycle = RandomSource { bound -> (i++) % bound }
        val counts = IntArray(6)
        repeat(6000) { counts[DiceEngine.roll(RollSpec(1, 6), cycle).dice[0] - 1]++ }
        assertTrue(counts.all { it == 1000 })
    }

    @Test fun rollRespectsModifiersAndBounds() {
        val src = SecureSource()
        repeat(1000) {
            val r = DiceEngine.roll(RollSpec(count = 3, sides = 6, modifier = 2), src)
            assertTrue(r.total in 5..20)
            assertEquals(3, r.dice.size)
            assertEquals(5, r.minPossible); assertEquals(20, r.maxPossible)
        }
    }

    @Test fun advantageTakesHigherAndDisadvantageLower() {
        val seq = listOf(3, 17).iterator()   // values are bound-relative: nextInt returns value-1 => dice 4 and 18
        val src = RandomSource { seq.next() }
        val adv = DiceEngine.roll(RollSpec(1, 20, mode = RollMode.ADVANTAGE), src)
        assertEquals(listOf(4, 18), adv.dice); assertEquals(18, adv.total)
        val seq2 = listOf(3, 17).iterator()
        val dis = DiceEngine.roll(RollSpec(1, 20, mode = RollMode.DISADVANTAGE), RandomSource { seq2.next() })
        assertEquals(4, dis.total)
    }

    @Test fun critsAndCoin() {
        val max = DiceEngine.roll(RollSpec(1, 20), RandomSource { 19 })
        assertEquals(Crit.SUCCESS, max.crit); assertEquals(RollTier.HIGH, max.tier)
        val min = DiceEngine.roll(RollSpec(1, 20), RandomSource { 0 })
        assertEquals(Crit.FAILURE, min.crit); assertEquals(RollTier.LOW, min.tier)
        val heads = DiceEngine.roll(RollSpec.COIN, RandomSource { 0 })
        assertEquals(true, heads.heads)
        var h = 0
        val s = SecureSource()
        repeat(20000) { if (DiceEngine.roll(RollSpec.COIN, s).heads == true) h++ }
        assertTrue("coin heads=$h", h in 9500..10500)
    }

    @Test fun classificationUsesConfigurableThresholds() {
        val t = TierThresholds()
        assertEquals(RollTier.LOW, DiceEngine.classify(1, 1, 20, t))
        assertEquals(RollTier.LOW_MEDIUM, DiceEngine.classify(7, 1, 20, t))
        assertEquals(RollTier.MEDIUM, DiceEngine.classify(10, 1, 20, t))
        assertEquals(RollTier.HIGH_MEDIUM, DiceEngine.classify(15, 1, 20, t))
        assertEquals(RollTier.HIGH, DiceEngine.classify(20, 1, 20, t))
        val strict = TierThresholds(50, 70, 85, 95)
        assertEquals(RollTier.LOW, DiceEngine.classify(10, 1, 20, strict))
        assertTrue(!TierThresholds(50, 40, 60, 80).isValid())
    }

    @Test fun auditChainIsDeterministicAndTamperEvident() {
        val g = AuditChain.genesis("c", "seed")
        val a = AuditChain.next(g, "r1", "1d20", listOf(7))
        assertEquals(a, AuditChain.next(g, "r1", "1d20", listOf(7)))
        assertNotEquals(a, AuditChain.next(g, "r1", "1d20", listOf(8)))
    }

    @Test fun invalidSpecsRejected() {
        assertFalse(RollSpec(0, 6).isValid()); assertFalse(RollSpec(1, 1).isValid()); assertFalse(RollSpec(51, 6).isValid())
        assertTrue(RollSpec(50, 1000).isValid())
    }
}

class StatsTest {
    private val base = GameCharacter(id = "c", campaignId = "k", name = "Hero", baseHp = 30, baseMp = 10, baseAtk = 5, baseDef = 3, baseSpd = 4)

    @Test fun finalStatsCombineBaseEquipmentAndStatus() {
        val sword = Item("i1", "k", "c", "Sword", equipped = true, bonusAtk = 4)
        val unequipped = Item("i2", "k", "c", "Spare", equipped = false, bonusAtk = 100)
        val shield = Item("i3", "k", "c", "Shield", equipped = true, bonusDef = 3, bonusHp = 5)
        val curse = StatusEffect("s1", "c", "Weakened", modAtk = -2, modSpd = -1)
        val f = StatCalculator.compute(base, listOf(sword, unequipped, shield), listOf(curse))
        assertEquals(7, f.atk.total); assertEquals(6, f.def.total); assertEquals(3, f.spd.total); assertEquals(35, f.maxHp)
        assertEquals(4, f.atk.equipment); assertEquals(-2, f.atk.status)
    }

    @Test fun luckIsABaseEquipmentAndStatusStat() {
        val clover = Item("i3", "k", "c", "Clover", equipped = true, bonusLuck = 4)
        val curse = StatusEffect("s2", "c", "Cursed", modLuck = -2)
        assertEquals(5 + 4 - 2, StatCalculator.compute(base, listOf(clover), listOf(curse)).luck.total)
        assertEquals(5, StatCalculator.compute(base, emptyList(), emptyList()).luck.total)
    }

    @Test fun levelUpsApplyAndCarryOverExperience() {
        assertEquals(100L, Leveling.expToNext(1))
        val (c, gained) = Leveling.award(base, 100 + 300 + 50)
        assertEquals(2, gained); assertEquals(3, c.level); assertEquals(50L, c.exp)
        assertEquals(40, c.baseHp); assertEquals(7, c.baseAtk)
    }
}

class AudienceTest {
    @Test fun audienceMath() {
        assertTrue(Aud.contains("*", "x")); assertTrue(Aud.contains("a,b", "b")); assertFalse(Aud.contains("a,b", "c"))
        assertEquals("*", Aud.union("a", "*")); assertEquals("a,b", Aud.union("a", "b"))
        assertEquals("DM", Aud.of())
    }
}
