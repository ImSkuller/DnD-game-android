package com.runehearth.app.engine

import com.runehearth.app.data.GameCharacter
import com.runehearth.app.data.Item
import com.runehearth.app.data.StatusEffect

/** One derived stat with its three contributing parts (base + equipment + temporary modifiers). */
data class StatLine(val base: Int, val equipment: Int, val status: Int) {
    val total: Int get() = base + equipment + status
}

data class FinalStats(val hp: StatLine, val mp: StatLine, val atk: StatLine, val def: StatLine, val spd: StatLine) {
    val maxHp: Int get() = hp.total.coerceAtLeast(1)
    val maxMp: Int get() = mp.total.coerceAtLeast(0)
}

object StatCalculator {
    /** Final stats = base + bonuses of equipped items + active status modifiers. Pure; unit tested. */
    fun compute(c: GameCharacter, equipped: List<Item>, statuses: List<StatusEffect>): FinalStats {
        var eHp = 0; var eMp = 0; var eAtk = 0; var eDef = 0; var eSpd = 0
        for (i in equipped) if (i.equipped) {
            eHp += i.bonusHp; eMp += i.bonusMp; eAtk += i.bonusAtk; eDef += i.bonusDef; eSpd += i.bonusSpd
        }
        var sHp = 0; var sMp = 0; var sAtk = 0; var sDef = 0; var sSpd = 0
        for (s in statuses) { sHp += s.modHp; sMp += s.modMp; sAtk += s.modAtk; sDef += s.modDef; sSpd += s.modSpd }
        return FinalStats(
            StatLine(c.baseHp, eHp, sHp), StatLine(c.baseMp, eMp, sMp), StatLine(c.baseAtk, eAtk, sAtk),
            StatLine(c.baseDef, eDef, sDef), StatLine(c.baseSpd, eSpd, sSpd),
        )
    }
}

object Leveling {
    const val MAX_LEVEL = 100
    const val HP_PER_LEVEL = 5
    const val MP_PER_LEVEL = 3
    const val STAT_PER_LEVEL = 1

    /** Experience needed to advance from [level] to level+1. */
    fun expToNext(level: Int): Long = 50L * level * (level + 1)

    /** Adds experience, applying every level-up it triggers. Returns the updated character and levels gained. */
    fun award(c: GameCharacter, amount: Long): Pair<GameCharacter, Int> {
        var ch = c.copy(exp = (c.exp + amount).coerceAtLeast(0))
        var gained = 0
        while (ch.level < MAX_LEVEL && ch.exp >= expToNext(ch.level)) {
            ch = ch.copy(
                exp = ch.exp - expToNext(ch.level),
                level = ch.level + 1,
                baseHp = ch.baseHp + HP_PER_LEVEL, baseMp = ch.baseMp + MP_PER_LEVEL,
                baseAtk = ch.baseAtk + STAT_PER_LEVEL, baseDef = ch.baseDef + STAT_PER_LEVEL,
                baseSpd = ch.baseSpd + STAT_PER_LEVEL,
            )
            gained++
        }
        if (ch.level >= MAX_LEVEL) ch = ch.copy(exp = 0)
        return ch to gained
    }
}
