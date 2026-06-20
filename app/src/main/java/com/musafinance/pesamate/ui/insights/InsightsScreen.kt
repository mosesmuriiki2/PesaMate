package com.musafinance.pesamate.ui.insights

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.musafinance.pesamate.data.local.TransactionEntity
import com.musafinance.pesamate.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val repository: TransactionRepository
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
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        repository.allTransactions,
        _dateRange,
        _searchQuery
    ) { txs, range, query ->
        txs.filter { it.date in range.first..range.second }
            .filter { 
                if (query.isBlank()) true
                else it.description.contains(query, ignoreCase = true) ||
                     it.receiver?.contains(query, ignoreCase = true) == true ||
                     it.sender?.contains(query, ignoreCase = true) == true ||
                     it.category.contains(query, ignoreCase = true)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val categoryData: StateFlow<Map<String, Double>> = filteredTransactions
        .map { txs ->
            txs.filter { it.type == "EXPENSE" }
                .groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    
    val incomeVsExpense: StateFlow<Pair<Double, Double>> = filteredTransactions
        .map { txs ->
            val income = txs.filter { it.type == "INCOME" }.sumOf { it.amount }
            val expense = txs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            Pair(income, expense)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(0.0, 0.0))
    
    // Top recipients - who you sent money to most
    val topRecipients: StateFlow<List<Pair<String, Double>>> = filteredTransactions
        .map { txs ->
            txs.filter { it.type == "EXPENSE" && !it.receiver.isNullOrBlank() }
                .groupBy { it.receiver!! }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
                .toList()
                .sortedByDescending { it.second }
                .take(10)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // Top senders - who sent you money most
    val topSenders: StateFlow<List<Pair<String, Double>>> = filteredTransactions
        .map { txs ->
            txs.filter { it.type == "INCOME" && !it.sender.isNullOrBlank() }
                .groupBy { it.sender!! }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
                .toList()
                .sortedByDescending { it.second }
                .take(10)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val avgDailySpending: StateFlow<Double> = filteredTransactions
        .map { txs ->
            val expenses = txs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val days = ((System.currentTimeMillis() - (_dateRange.value.first)) / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
            expenses / days
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val loanStatus: StateFlow<Map<String, Double>> = repository.allLoans
        .map { loans ->
            mapOf(
                "Total Borrowed" to loans.sumOf { it.amountBorrowed },
                "Total Repaid" to loans.sumOf { it.amountRepaid },
                "Outstanding" to loans.sumOf { it.outstandingBalance }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    
    fun updateDateRange(start: Long, end: Long) {
        _dateRange.value = Pair(start, end)
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(viewModel: InsightsViewModel = hiltViewModel()) {
    val categoryMap by viewModel.categoryData.collectAsState()
    val incomeVsExpense by viewModel.incomeVsExpense.collectAsState()
    val topRecipients by viewModel.topRecipients.collectAsState()
    val topSenders by viewModel.topSenders.collectAsState()
    val avgDailySpending by viewModel.avgDailySpending.collectAsState()
    val loanStatus by viewModel.loanStatus.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val dateRange by viewModel.dateRange.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Financial Insights", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Filter Date")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search transactions, people, categories...") },
                leadingIcon = { 
                    Icon(Icons.Default.Search, contentDescription = "Search") 
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true
            )
            
            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Overview") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Recipients") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Senders") }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Loans") }
                )
            }
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                
                when (selectedTab) {
                    0 -> {
                        // Overview Tab
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SummaryCard(
                                    title = "Income",
                                    amount = incomeVsExpense.first,
                                    icon = Icons.Default.TrendingUp,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                SummaryCard(
                                    title = "Expenses",
                                    amount = incomeVsExpense.second,
                                    icon = Icons.Default.TrendingDown,
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Avg Daily Spending", style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "KSh ${String.format("%,.2f", avgDailySpending)}",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                        
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Spending by Category",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    if (categoryMap.isNotEmpty()) {
                                        CategoryPieChart(categoryMap)
                                    } else {
                                        EmptyState("No expense data")
                                    }
                                }
                            }
                        }
                        
                        item { CategoryListCard(categoryMap) }
                        
                        item { SmartInsightsCard(categoryMap, incomeVsExpense) }
                    }
                    
                    1 -> {
                        // Top Recipients Tab
                        item {
                            Text(
                                "People you sent money to most",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        
                        if (topRecipients.isEmpty()) {
                            item { EmptyState("No recipients found") }
                        } else {
                            items(topRecipients.size) { index ->
                                val (recipient, amount) = topRecipients[index]
                                RecipientCard(
                                    rank = index + 1,
                                    name = recipient,
                                    amount = amount,
                                    isTopThree = index < 3
                                )
                            }
                        }
                    }
                    
                    2 -> {
                        // Top Senders Tab
                        item {
                            Text(
                                "People who sent you money most",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        
                        if (topSenders.isEmpty()) {
                            item { EmptyState("No senders found") }
                        } else {
                            items(topSenders.size) { index ->
                                val (sender, amount) = topSenders[index]
                                RecipientCard(
                                    rank = index + 1,
                                    name = sender,
                                    amount = amount,
                                    isTopThree = index < 3,
                                    isIncoming = true
                                )
                            }
                        }
                    }

                    3 -> {
                        // Loans Tab
                        item { LoanAnalyticsCard(loanStatus) }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(16.dp)) }
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
                            viewModel.updateDateRange(start, end)
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
            DateRangePicker(state = datePickerState)
        }
    }
}

@Composable
fun CategoryPieChart(data: Map<String, Double>) {
    AndroidView(
        factory = { context ->
            PieChart(context).apply {
                description.isEnabled = false
                isDrawHoleEnabled = true
                setHoleColor(android.graphics.Color.TRANSPARENT)
                setEntryLabelColor(android.graphics.Color.BLACK)
                legend.isEnabled = false
            }
        },
        modifier = Modifier.fillMaxWidth().height(300.dp),
        update = { chart ->
            val entries = data.map { PieEntry(it.value.toFloat(), it.key) }
            val dataSet = PieDataSet(entries, "Categories").apply {
                colors = ColorTemplate.MATERIAL_COLORS.toList()
                valueTextSize = 12f
                valueTextColor = android.graphics.Color.BLACK
            }
            chart.data = PieData(dataSet)
            chart.invalidate()
        }
    )
}
