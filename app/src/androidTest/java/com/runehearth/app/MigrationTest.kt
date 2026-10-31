package com.runehearth.app

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.runehearth.app.data.GameDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** v1 -> v2 (v0.1.1): existing characters keep their data, gain Luck = 5, lose the six attribute columns. */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(), GameDatabase::class.java, listOf(GameDatabase.Migration1To2()), FrameworkSQLiteOpenHelperFactory(),
    )

    @Test fun migrateV1ToV2KeepsDataAndAddsLuck() {
        helper.createDatabase("mig", 1).apply {
            execSQL("INSERT INTO character (id,campaignId,name,art,level,exp,className,job,race,background,description,baseHp,baseMp,baseAtk,baseDef,baseSpd,curHp,curMp,str,dex,con,intel,wis,cha,notes,ownerDeviceId,isNpc,rev) " +
                "VALUES ('h','c','Old Hero','',7,12,'Rogue','','Elf','','',40,15,6,4,9,33,10,12,14,11,10,9,8,'my notes',NULL,0,3)")
            execSQL("INSERT INTO item (id,campaignId,ownerId,name,kind,slot,rarity,description,quantity,weight,art,equipped,bonusHp,bonusMp,bonusAtk,bonusDef,bonusSpd,healHp,healMp,value,rev) " +
                "VALUES ('i','c','h','Sword','WEAPON','MAIN_HAND','COMMON','',1,3,'',1,0,0,4,0,0,0,0,0,1)")
            close()
        }
        val db = helper.runMigrationsAndValidate("mig", 2, true)
        db.query("SELECT name, level, baseHp, baseSpd, notes, baseLuck FROM character WHERE id='h'").use { c ->
            c.moveToFirst()
            assertEquals("Old Hero", c.getString(0)); assertEquals(7, c.getInt(1)); assertEquals(40, c.getInt(2)); assertEquals(9, c.getInt(3))
            assertEquals("my notes", c.getString(4)); assertEquals(5, c.getInt(5))
        }
        db.query("SELECT bonusAtk, bonusLuck FROM item WHERE id='i'").use { c -> c.moveToFirst(); assertEquals(4, c.getInt(0)); assertEquals(0, c.getInt(1)) }
    }
}
