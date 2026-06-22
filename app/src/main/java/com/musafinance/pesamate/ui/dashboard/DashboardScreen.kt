package com.musafinance.pesamate.ui.dashboard

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.hilt.navigation.compose.hiltViewModel
import com.musafinance.pesamate.data.local.TransactionEntity
import com.musafinance.pesamate.data.repository.TransactionRepository
import com.musafinance.pesamate.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.*

data class DashboardSummary(
    val netWorth: Double,
    val monthlyIncome: Double,
    val monthlyExpenses: Double,
    val outstandingLoans: Double,
    val totalBudget: Double,
    val totalSpent: Double
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _dateRange = MutableStateFlow<Pair<Long, Long>>(
        Pair(
            Calendar.getInstance().apply { 
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0) 
            }.timeInMillis,
            System.currentTimeMillis()
        )
    )
    val dateRange = _dateRange.asStateFlow()
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    val transactions: StateFlow<List<TransactionEntity>> = combine(
        repository.allTransactions,
        _dateRange
    ) { txs, range ->
        txs.filter { it.date in range.first..range.second }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val summaryData: StateFlow<DashboardSummary> = combine(
        transactions,
        repository.allBudgets
    ) { txs, budgets ->
        val income = txs.filter { it.type == "INCOME" }.sumOf { it.amount }
        val expenses = txs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val net = income - expenses
        val budgetTotal = budgets.sumOf { it.limitAmount }
        val budgetSpent = budgets.sumOf { it.spentAmount }
        
        DashboardSummary(
            netWorth = net,
            monthlyIncome = income,
            monthlyExpenses = expenses,
            outstandingLoans = 0.0,
            totalBudget = budgetTotal,
            totalSpent = budgetSpent
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DashboardSummary(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    )
    
    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.syncFromFirestore()
                syncManager.triggerImmediateSync()
                delay(2000)
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error refreshing data", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun setDateRange(start: Long, end: Long) {
        _dateRange.value = Pair(start, end)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    onNavigateToExpenses: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val transactionList by viewModel.transactions.collectAsState()
    val sumSummary by viewModel.summaryData.collectAsState()
    val dateRange by viewModel.dateRange.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PesaMate", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.refreshData() }, enabled = !isRefreshing) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Filled.DateRange, contentDescription = "Filter Date")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                    Text(
                        text = "Showing: ${sdf.format(Date(dateRange.first))} - ${sdf.format(Date(dateRange.second))}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    val pagerState = rememberPagerState(pageCount = { 3 })
                    Column {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxWidth().height(160.dp)
                        ) { page ->
                            InsightsBanner(page, sumSummary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(3) { iteration ->
                                val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else Color.LightGray
                                Box(
                                    modifier = Modifier
                                        .padding(2.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(color)
                                        .size(8.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    AnimatedVisibility(visible = true, enter = fadeIn() + expandVertically()) {
                        BalanceWidget(
                            balance = sumSummary.netWorth,
                            income = sumSummary.monthlyIncome,
                            expenses = sumSummary.monthlyExpenses
                        )
                    }
                }

                if (sumSummary.totalBudget > 0) {
                    item {
                        AnimatedVisibility(visible = true, enter = slideInHorizontally() + fadeIn()) {
                            BudgetWidget(
                                totalBudget = sumSummary.totalBudget,
                                totalSpent = sumSummary.totalSpent
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Recent Transactions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(transactionList.take(20), key = { it.id }) { tx ->
                    AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically { it / 2 }) {
                        TransactionRow(tx = tx, onClick = { selectedTransaction = tx })
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedStartDateMillis?.let { start ->
                        datePickerState.selectedEndDateMillis?.let { end ->
                            viewModel.setDateRange(start, end)
                        }
                    }
                    showDatePicker = false
                }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DateRangePicker(state = datePickerState, modifier = Modifier.weight(1f))
        }
    }
    
    selectedTransaction?.let { transaction ->
        TransactionDetailDialog(transaction = transaction, onDismiss = { selectedTransaction = null })
    }
}

@Composable
fun InsightsBanner(page: Int, summary: DashboardSummary) {
    Card(
        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (page) {
                0 -> {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("Monthly Savings", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("KSh ${String.format(Locale.getDefault(), "%,.0f", summary.monthlyIncome - summary.monthlyExpenses)}", fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
                1 -> {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("PesaMate Insight", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    val percent = if (summary.totalBudget > 0) (summary.totalSpent / summary.totalBudget * 100).toInt() else 0
                    Text("$percent% budget used", fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
                2 -> {
                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("Monthly Expenses", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("KSh ${String.format(Locale.getDefault(), "%,.0f", summary.monthlyExpenses)}", fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun BalanceWidget(balance: Double, income: Double, expenses: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Total Balance",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
            )
            Text(
                text = "KSh ${String.format(Locale.getDefault(), "%,.2f", balance)}",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryItem(label = "Income", amount = income, icon = Icons.Default.ArrowDownward, contentColor = MaterialTheme.colorScheme.onPrimary)
                VerticalDivider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f), modifier = Modifier.height(40.dp))
                SummaryItem(label = "Expenses", amount = expenses, icon = Icons.Default.ArrowUpward, contentColor = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
fun SummaryItem(label: String, amount: Double, icon: androidx.compose.ui.graphics.vector.ImageVector, contentColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, fontSize = 12.sp, color = contentColor.copy(alpha = 0.7f))
        }
        Text(text = "KSh ${String.format(Locale.getDefault(), "%,.0f", amount)}", color = contentColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun TransactionRow(tx: TransactionEntity, onClick: () -> Unit = {}) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(color = if (tx.type == "INCOME") Color(0xFFE8F5E9) else Color(0xFFFFEBEE), shape = androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when(tx.category.lowercase()) {
                        "subscription" -> Icons.Default.NotificationsActive
                        "food", "groceries", "dining out" -> Icons.Default.Restaurant
                        "transport", "taxi", "fuel" -> Icons.Default.DirectionsCar
                        "utilities", "electricity", "water" -> Icons.Default.Power
                        "loan", "banking" -> Icons.Default.AccountBalance
                        "entertainment" -> Icons.Default.Movie
                        "health" -> Icons.Default.MedicalServices
                        "education" -> Icons.Default.School
                        "airtime" -> Icons.Default.PhoneAndroid
                        "savings" -> Icons.Default.Savings
                        else -> if (tx.type == "INCOME") Icons.Default.Add else Icons.Default.Payments
                    },
                    contentDescription = null,
                    tint = if (tx.type == "INCOME") Color(0xFF2E7D32) else Color(0xFFC62828),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = tx.description, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(text = tx.category, fontSize = 14.sp, color = Color.Gray)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "${if (tx.type == "INCOME") "+" else "-"} KSh ${String.format(Locale.getDefault(), "%,.2f", tx.amount)}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = if (tx.type == "INCOME") Color(0xFF2E7D32) else Color(0xFFC62828))
                val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                Text(text = sdf.format(Date(tx.date)), fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun TransactionDetailDialog(transaction: TransactionEntity, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transaction Details", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailRow("Code", transaction.transactionCode ?: "N/A")
                DetailRow("Amount", "KSh ${String.format("%,.2f", transaction.amount)}")
                DetailRow("Type", transaction.type)
                DetailRow("Category", transaction.category)
                DetailRow("Description", transaction.description)
                DetailRow("Provider", transaction.provider)
                transaction.sender?.let { DetailRow("From", it) }
                transaction.receiver?.let { DetailRow("To", it) }
                transaction.balance?.let { DetailRow("Balance After", "KSh ${String.format("%,.2f", it)}") }
                DetailRow("Date", java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(transaction.date)))
                transaction.accountName?.let { DetailRow("Account", it) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Color.Gray, modifier = Modifier.weight(0.4f))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.6f))
    }
}

@Composable
fun BudgetWidget(totalBudget: Double, totalSpent: Double) {
    val progress = if (totalBudget > 0) (totalSpent / totalBudget).toFloat() else 0f
    val color = when {
        progress > 0.9f -> Color.Red
        progress > 0.7f -> Color.Yellow
        else -> Color.Green
    }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Range Budget Status", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(8.dp), color = color)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Spent: KSh $totalSpent", fontSize = 12.sp)
                Text("Limit: KSh $totalBudget", fontSize = 12.sp)
            }
        }
    }
}
