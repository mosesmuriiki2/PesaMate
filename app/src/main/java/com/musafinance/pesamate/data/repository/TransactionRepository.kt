package com.musafinance.pesamate.data.repository


import android.util.Log
import com.musafinance.pesamate.data.local.*
import com.musafinance.pesamate.notifications.SpendingLimitMonitor
import com.musafinance.pesamate.notifications.NotificationHelper
import com.musafinance.pesamate.ui.theme.ThemePreferences
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val pesamateDao: PesaMateDao,
    private val firestore: FirebaseFirestore,
    private val spendingLimitMonitor: SpendingLimitMonitor,
    private val themePreferences: ThemePreferences,
    @ApplicationContext private val context: Context
) {
    val allTransactions: Flow<List<TransactionEntity>> = pesamateDao.getAllTransactionsFlow()
    val allLoans: Flow<List<LoanEntity>> = pesamateDao.getAllLoansFlow()
    val allBudgets: Flow<List<BudgetEntity>> = pesamateDao.getAllBudgetsFlow()
    val allSubscriptions: Flow<List<SubscriptionEntity>> = pesamateDao.getAllSubscriptionsFlow()
    val allSavings: Flow<List<SavingsEntity>> = pesamateDao.getAllSavingsFlow()

    suspend fun saveTransaction(tx: TransactionEntity) {
        // Save to offline cache (Room)
        pesamateDao.insertTransaction(tx)

        // Handle Savings updates automatically
        val savingsKeywords = listOf("M-Shwari", "KCB M-PESA", "Lock", "Savings", "Bank")
        val isSavings = tx.type == "SAVINGS" || tx.category.contains("Savings", true) || savingsKeywords.any { tx.description.contains(it, true) }
        
        if (isSavings) {
            val accountId = tx.receiver ?: tx.provider
            val existingSavings = allSavings.first().find { it.id == accountId }
            if (existingSavings != null) {
                pesamateDao.updateSavingsBalance(accountId, tx.amount)
            } else {
                pesamateDao.insertSavings(SavingsEntity(
                    id = accountId,
                    accountName = accountId,
                    balance = tx.amount,
                    provider = tx.provider,
                    lastUpdated = System.currentTimeMillis()
                ))
            }
        } else if (tx.category.contains("Withdrawal", true)) {
            val accountId = tx.sender ?: tx.provider
            pesamateDao.updateSavingsBalance(accountId, -tx.amount)
        }

        // Show notification for new transaction
        NotificationHelper.showTransactionNotification(
            context,
            if (tx.type == "INCOME") "Money Received" else "Payment Confirmed",
            "${tx.description}: KSh ${String.format(java.util.Locale.getDefault(), "%,.2f", tx.amount)}"
        )

        // Update Budget Category limits
        if (tx.type == "EXPENSE") {
            pesamateDao.updateBudgetSpending(tx.category, tx.amount)
            
            // Auto-detect subscriptions
            if (tx.category == "Subscription") {
                val sub = SubscriptionEntity(
                    id = tx.receiver ?: tx.id,
                    name = tx.description,
                    amount = tx.amount,
                    category = tx.category,
                    nextBillingDate = tx.date + (30L * 24 * 60 * 60 * 1000), // Default +30 days
                    frequency = "Monthly"
                )
                pesamateDao.insertSubscription(sub)
            }

            // Check daily spending limit
            spendingLimitMonitor.checkDailyLimit()
        }

        // Cloud sync if enabled
        if (themePreferences.isCloudSyncEnabled.first()) {
            try {
                firestore.collection("transactions")
                    .document(tx.id)
                    .set(tx.copy(isSynced = true))
                    .await()
                pesamateDao.insertTransaction(tx.copy(isSynced = true))
            } catch (e: Exception) {
                Log.e("TransactionRepository", "Error syncing transaction", e)
            }
        }
    }

    suspend fun saveLoan(loan: LoanEntity) {
        pesamateDao.insertLoan(loan)
        if (themePreferences.isCloudSyncEnabled.first()) {
            try {
                firestore.collection("loans")
                    .document(loan.id)
                    .set(loan.copy(isSynced = true))
                    .await()
                pesamateDao.insertLoan(loan.copy(isSynced = true))
            } catch (e: Exception) {
                Log.e("TransactionRepository", "Error syncing loan", e)
            }
        }
    }

    suspend fun deleteLoan(id: String) {
        pesamateDao.deleteLoan(id)
        if (themePreferences.isCloudSyncEnabled.first()) {
            try {
                firestore.collection("loans").document(id).delete().await()
            } catch (e: Exception) {
                Log.e("TransactionRepository", "Error deleting loan from cloud", e)
            }
        }
    }

    suspend fun saveBudget(budget: BudgetEntity) {
        pesamateDao.insertBudget(budget)
    }

    suspend fun deleteBudget(id: String) {
        pesamateDao.deleteBudget(id)
    }

    suspend fun saveSubscription(subscription: SubscriptionEntity) {
        pesamateDao.insertSubscription(subscription)
    }

    suspend fun deleteSubscription(subscription: SubscriptionEntity) {
        pesamateDao.deleteSubscription(subscription)
    }

    suspend fun saveSavings(savings: SavingsEntity) {
        pesamateDao.insertSavings(savings)
    }

    suspend fun deleteSavings(id: String) {
        pesamateDao.deleteSavings(id)
    }
    
    suspend fun saveDailyLimit(limit: Double) {
        pesamateDao.insertDailyLimit(DailyLimitEntity(limitAmount = limit))
    }
    
    suspend fun getDailyLimit(): DailyLimitEntity? {
        return pesamateDao.getDailyLimit()
    }
    
    suspend fun syncFromFirestore() {
        if (!themePreferences.isCloudSyncEnabled.first()) return
        try {
            Log.d("TransactionRepository", "Fetching transactions from Firestore...")
            val snapshot = firestore.collection("transactions")
                .get()
                .await()
            
            val transactions = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(TransactionEntity::class.java)
                } catch (e: Exception) {
                    Log.e("TransactionRepository", "Error parsing transaction: ${doc.id}", e)
                    null
                }
            }
            
            transactions.forEach { tx ->
                pesamateDao.insertTransaction(tx.copy(isSynced = true))
            }
            
            // Fetch loans
            val loansSnapshot = firestore.collection("loans")
                .get()
                .await()
            
            val loans = loansSnapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(LoanEntity::class.java)
                } catch (e: Exception) {
                    null
                }
            }
            
            loans.forEach { loan ->
                pesamateDao.insertLoan(loan.copy(isSynced = true))
            }
            
        } catch (e: Exception) {
            Log.e("TransactionRepository", "Error syncing from Firestore", e)
        }
    }

    suspend fun syncUnsyncedData() {
        if (!themePreferences.isCloudSyncEnabled.first()) return
        try {
            val unsyncedTransactions = allTransactions.first().filter { !it.isSynced }
            for (tx in unsyncedTransactions) {
                firestore.collection("transactions")
                    .document(tx.id)
                    .set(tx.copy(isSynced = true))
                    .await()
                pesamateDao.insertTransaction(tx.copy(isSynced = true))
            }
            
            val unsyncedLoans = allLoans.first().filter { !it.isSynced }
            for (loan in unsyncedLoans) {
                firestore.collection("loans")
                    .document(loan.id)
                    .set(loan.copy(isSynced = true))
                    .await()
                pesamateDao.insertLoan(loan.copy(isSynced = true))
            }
        } catch (e: Exception) {
            Log.e("TransactionRepository", "Error syncing unsynced data", e)
        }
    }
    
    suspend fun getTransactionsByDateRange(startDate: Long, endDate: Long): List<TransactionEntity> {
        return allTransactions.first().filter { it.date in startDate..endDate }
    }

    suspend fun checkLoanDeadlines() {
        val loans = allLoans.first()
        val now = System.currentTimeMillis()
        loans.forEach { loan ->
            val daysUntilDue = ((loan.dueDate - now) / (1000 * 60 * 60 * 24)).toInt()
            if (loan.outstandingBalance > 0 && daysUntilDue <= 3) {
                NotificationHelper.showLoanDueNotification(
                    context,
                    loan.lender,
                    loan.outstandingBalance,
                    daysUntilDue
                )
            }
        }
    }
}
