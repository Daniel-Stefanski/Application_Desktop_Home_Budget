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
import com.example.homebudget.data.dao.PendingSyncDao
import com.example.homebudget.data.entity.PendingSync
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
    entities = [User::class, Settings::class, Expense::class, SavingsGoal::class, MonthlyBudget::class, Contribution::class, PendingSync::class],
    version = 46,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun settingsDao(): SettingsDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun monthlyBudgetDao(): MonthlyBudgetDao
    abstract fun contributionDao(): ContributionDao
    abstract fun pendingSyncDao(): PendingSyncDao

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
                addColumnIfMissing(connection, "settings", "defaultPaymentMethod", "TEXT")
            }
        }

        private val MIGRATION_44_45 = object : Migration(44, 45) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pending_sync (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        entityType TEXT NOT NULL,
                        operation TEXT NOT NULL,
                        localId INTEGER,
                        remoteId INTEGER,
                        payloadJson TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                addColumnIfMissing(connection, "expenses", "remoteId", "INTEGER")
                addColumnIfMissing(connection, "savings_goals", "remoteId", "INTEGER")
                addColumnIfMissing(connection, "contributions", "remoteId", "INTEGER")
                rebuildMonthlyBudgetsIfNeeded(connection)
            }
        }

        private val MIGRATION_45_46 = object : Migration(45, 46) {
            override fun migrate(connection: SQLiteConnection) {
                rebuildMonthlyBudgetsIfNeeded(connection)
            }
        }

        private fun addColumnIfMissing(
            connection: SQLiteConnection,
            table: String,
            column: String,
            type: String
        ) {
            val cursor = connection.prepare("PRAGMA table_info($table)")
            var exists = false
            while (cursor.step()) {
                if (cursor.getText(1) == column) {
                    exists = true
                    break
                }
            }
            cursor.close()
            if (!exists) {
                connection.execSQL("ALTER TABLE $table ADD COLUMN $column $type")
            }
        }

        private fun rebuildMonthlyBudgetsIfNeeded(connection: SQLiteConnection) {
            val cursor = connection.prepare("PRAGMA table_info(monthly_budgets)")
            var hasIdColumn = false
            var idIsPrimaryKey = false
            while (cursor.step()) {
                if (cursor.getText(1) == "id") {
                    hasIdColumn = true
                    idIsPrimaryKey = cursor.getLong(5) == 1L
                    break
                }
            }
            cursor.close()

            if (hasIdColumn && idIsPrimaryKey) return

            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS monthly_budgets_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId INTEGER NOT NULL,
                    year INTEGER NOT NULL,
                    month INTEGER NOT NULL,
                    budget REAL NOT NULL,
                    isDefault INTEGER NOT NULL
                )
                """.trimIndent()
            )
            connection.execSQL(
                """
                INSERT INTO monthly_budgets_new (userId, year, month, budget, isDefault)
                SELECT userId, year, month, budget, isDefault
                FROM monthly_budgets
                """.trimIndent()
            )
            connection.execSQL("DROP TABLE monthly_budgets")
            connection.execSQL("ALTER TABLE monthly_budgets_new RENAME TO monthly_budgets")
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
                    .addMigrations(MIGRATION_43_44, MIGRATION_44_45, MIGRATION_45_46)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
