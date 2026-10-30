package com.runehearth.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Campaign::class, GameCharacter::class, Item::class, Skill::class, StatusEffect::class, Wallet::class,
        MoneyTx::class, Entry::class, Encounter::class, Combatant::class, RollRecord::class, EventLogEntry::class,
        Change::class, Member::class, ProcessedAction::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class GameDatabase : RoomDatabase() {
    abstract fun dao(): GameDao

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
