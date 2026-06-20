package com.musafinance.pesamate.data.local


import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

// Type converters for Date and TransactionType
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String = "",
    val amount: Double = 0.0,
    val date: Long = 0L,
    val type: String = "", // INCOME, EXPENSE, LOAN_DISBURSEMENT, SAVINGS, etc.
    val category: String = "",
    val description: String = "",
    val transactionCode: String? = null,
    val sender: String? = null,
    val receiver: String? = null,
    val accountName: String? = null,
    val balance: Double? = null,
    val provider: String = "",
    val isManual: Boolean = false,
    val isSynced: Boolean = false
)

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey val id: String = "",
    val lender: String = "",
    val amountBorrowed: Double = 0.0,
    val amountRepaid: Double = 0.0,
    val outstandingBalance: Double = 0.0,
    val dueDate: Long = 0L,
    val interestRate: Double = 0.0,
    val isManual: Boolean = false,
    val isSynced: Boolean = false
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: String = "",
    val category: String = "",
    val limitAmount: Double = 0.0,
    val spentAmount: Double = 0.0
)

@Entity(tableName = "daily_limits")
data class DailyLimitEntity(
    @PrimaryKey val id: Int = 0, // Single row for global daily limit
    val limitAmount: Double = 0.0
)

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val nextBillingDate: Long = 0L,
    val frequency: String = "", // Monthly, Weekly, Yearly
    val isAutoTracked: Boolean = true
)

@Entity(tableName = "savings")
data class SavingsEntity(
    @PrimaryKey val id: String = "",
    val accountName: String = "",
    val balance: Double = 0.0,
    val provider: String = "",
    val lastUpdated: Long = 0L
)

@Dao
interface PesaMateDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM loans ORDER BY dueDate ASC")
    fun getAllLoansFlow(): Flow<List<LoanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity)

    @Update
    suspend fun updateLoan(loan: LoanEntity)

    @Query("SELECT * FROM budgets")
    fun getAllBudgetsFlow(): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    @Query("UPDATE budgets SET spentAmount = spentAmount + :amount WHERE category = :category")
    suspend fun updateBudgetSpending(category: String, amount: Double)

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE' AND category = :category AND date >= :startDate")
    suspend fun getCategorySpendingSince(category: String, startDate: Long): Double?

    @Query("SELECT * FROM daily_limits WHERE id = 0")
    suspend fun getDailyLimit(): DailyLimitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyLimit(limit: DailyLimitEntity)

    @Query("SELECT * FROM subscriptions")
    fun getAllSubscriptionsFlow(): Flow<List<SubscriptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: SubscriptionEntity)

    @Delete
    suspend fun deleteSubscription(subscription: SubscriptionEntity)

    @Query("SELECT * FROM savings")
    fun getAllSavingsFlow(): Flow<List<SavingsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavings(savings: SavingsEntity)

    @Query("UPDATE savings SET balance = balance + :amount WHERE id = :id")
    suspend fun updateSavingsBalance(id: String, amount: Double)

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE' AND date >= :startOfDay")
    suspend fun getTotalSpendingForDay(startOfDay: Long): Double?

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudget(id: String)

    @Query("DELETE FROM loans WHERE id = :id")
    suspend fun deleteLoan(id: String)
    
    @Query("DELETE FROM savings WHERE id = :id")
    suspend fun deleteSavings(id: String)
}

@Database(entities = [TransactionEntity::class, LoanEntity::class, BudgetEntity::class, DailyLimitEntity::class, SubscriptionEntity::class, SavingsEntity::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class PesaMateDatabase : RoomDatabase() {
    abstract fun pesamateDao(): PesaMateDao
}
