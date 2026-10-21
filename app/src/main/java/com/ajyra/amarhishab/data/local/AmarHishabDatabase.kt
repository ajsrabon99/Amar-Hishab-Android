package com.ajyra.amarhishab.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TransactionEntity::class, SavingsGoalEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AmarHishabDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun savingsGoalDao(): SavingsGoalDao

    companion object {
        @Volatile
        private var INSTANCE: AmarHishabDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `savings_goals` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `targetAmount` REAL NOT NULL,
                        `savedAmount` REAL NOT NULL,
                        `targetDate` TEXT NOT NULL,
                        `description` TEXT,
                        `iconCategory` TEXT,
                        `isActiveOnDashboard` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `workspace` TEXT NOT NULL DEFAULT 'PERSONAL'")
                db.execSQL("ALTER TABLE `savings_goals` ADD COLUMN `workspace` TEXT NOT NULL DEFAULT 'PERSONAL'")
            }
        }

        fun getInstance(context: Context): AmarHishabDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AmarHishabDatabase::class.java,
                    "amar_hishab_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

