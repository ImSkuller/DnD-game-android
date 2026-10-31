package com.runehearth.app.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.migration.AutoMigrationSpec
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Campaign::class, GameCharacter::class, Item::class, Skill::class, StatusEffect::class, Wallet::class,
        MoneyTx::class, Entry::class, Encounter::class, Combatant::class, RollRecord::class, EventLogEntry::class,
        Change::class, Member::class, ProcessedAction::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2, spec = GameDatabase.Migration1To2::class)],
)
abstract class GameDatabase : RoomDatabase() {
    abstract fun dao(): GameDao

    /** v0.1.1: the six core stats (HP, Mana, Attack, Defense, Speed, Luck). Luck columns are added; the six attribute columns are dropped. */
    @DeleteColumn.Entries(
        DeleteColumn(tableName = "character", columnName = "str"), DeleteColumn(tableName = "character", columnName = "dex"),
        DeleteColumn(tableName = "character", columnName = "con"), DeleteColumn(tableName = "character", columnName = "intel"),
        DeleteColumn(tableName = "character", columnName = "wis"), DeleteColumn(tableName = "character", columnName = "cha"),
    )
    class Migration1To2 : AutoMigrationSpec

    companion object {
        const val NAME = "runehearth.db"

        /** Future migrations are registered here; each must have a test in androidTest/MigrationTest. */
        val MIGRATIONS: Array<androidx.room.migration.Migration> = arrayOf()

        fun build(context: Context): GameDatabase =
            Room.databaseBuilder(context.applicationContext, GameDatabase::class.java, NAME)
                .addMigrations(*MIGRATIONS)
                .build() // WAL is the default journal mode on API 16+ non-low-RAM devices; fast & crash safe.
    }
}
