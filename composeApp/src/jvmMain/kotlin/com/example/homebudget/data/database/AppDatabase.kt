package com.example.homebudget.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.example.homebudget.data.entity.Contribution
import com.example.homebudget.data.dao.ContributionDao
import com.example.homebudget.data.entity.Expense
import com.example.homebudget.data.dao.ExpenseDao
import com.example.homebudget.data.entity.MonthlyBudget
import com.example.homebudget.data.dao.MonthlyBudgetDao
import com.example.homebudget.data.entity.SavingsGoal
import com.example.homebudget.data.dao.SavingsGoalDao
import com.example.homebudget.data.entity.Settings
import com.example.homebudget.data.dao.SettingsDao
import com.example.homebudget.data.entity.User
import com.example.homebudget.data.dao.UserDao
import kotlinx.coroutines.Dispatchers
import java.io.File

//AppDatabase.kt – główna klasa Room Database łącząca wszystkie DAO.
@Database(
    entities = [User::class, Settings::class, Expense::class, SavingsGoal::class, MonthlyBudget::class, Contribution::class],
    version = 44,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun settingsDao(): SettingsDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun monthlyBudgetDao(): MonthlyBudgetDao
    abstract fun contributionDao(): ContributionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        /**
         * Migracja 43 -> 44 (Room Multiplatform/Desktop)
         * Dodaje nowe kolumny do tabeli na przykład settings
         * DANE NIE SĄ USUWANE
         */
        private val MIGRATION_43_44 = object : Migration(43, 44) {
            override fun migrate(connection: SQLiteConnection) {
                // Sprawdzamy czy kolumna już istnieje dla danej tabeli na przykład settings
                val cursor = connection.prepare("PRAGMA table_info(settings)")
                var hasDefaultCategory = false
                while (cursor.step()) {
                    val columnName = cursor.getText(1)
                    if (columnName == "defaultCategory") {
                        hasDefaultCategory = true
                        break
                    }
                }
                cursor.close()
                if (!hasDefaultCategory) {
                    // Przykład: Dodajemy kolumnę do danej tabeli czyli Dane zostają, nowe kolumny są NULL dla starych rekordów
                    connection.execSQL("ALTER TABLE settings ADD COLUMN defaultCategory TEXT")
                }
            }
        }

        fun getDatabase(): AppDatabase {
            return INSTANCE ?: synchronized(this) {

                val dbFile = File(
                    System.getProperty("user.home"),
                    ".homebudget/home_budget_db.db"
                )
                dbFile.parentFile.mkdirs()

                val instance = Room.databaseBuilder<AppDatabase>(
                    name = dbFile.absolutePath
                )
                    .setDriver(BundledSQLiteDriver())
                    .setQueryCoroutineContext(Dispatchers.IO)
                    .addMigrations(MIGRATION_43_44)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}