package com.musafinance.pesamate.ui.reports

import android.content.Context
import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.musafinance.pesamate.utils.ExportHelper
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.musafinance.pesamate.data.local.TransactionEntity
import com.musafinance.pesamate.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
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
    
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        repository.allTransactions,
        _dateRange
    ) { txs, range ->
        txs.filter { it.date in range.first..range.second }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val categorySpending: StateFlow<Map<String, Double>> = filteredTransactions
        .map { list ->
            list.filter { it.type == "EXPENSE" }
                .groupBy { it.category }
                .mapValues { (_, txs) -> txs.sumOf { it.amount } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    
    val monthlyTrend: StateFlow<Map<String, Pair<Double, Double>>> = filteredTransactions
        .map { list ->
            list.groupBy { 
                SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(it.date))
            }.mapValues { (_, txs) ->
                val income = txs.filter { it.type == "INCOME" }.sumOf { it.amount }
                val expense = txs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                Pair(income, expense)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val incomeSources: StateFlow<Map<String, Double>> = filteredTransactions
        .map { list ->
            list.filter { it.type == "INCOME" }
                .groupBy { it.sender ?: "Other" }
                .mapValues { (_, txs) -> txs.sumOf { it.amount } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val topExpenseGroups: StateFlow<Map<String, Double>> = filteredTransactions
        .map { list ->
            list.filter { it.type == "EXPENSE" }
                .groupBy { tx ->
                    val receiver = tx.receiver?.lowercase() ?: "other"
                    when {
                        receiver.contains("supermarket") || receiver.contains("naivas") || receiver.contains("carrefour") || receiver.contains("quick") || receiver.contains("mart") -> "Supermarkets"
                        receiver.contains("kplc") || receiver.contains("power") || receiver.contains("water") || receiver.contains("token") -> "Utilities"
                        receiver.contains("netflix") || receiver.contains("spotify") || receiver.contains("showmax") || receiver.contains("sub") -> "Subscriptions"
                        receiver.contains("uber") || receiver.contains("bolt") || receiver.contains("taxi") || receiver.contains("transport") || receiver.contains("matatu") -> "Transport"
                        receiver.contains("java") || receiver.contains("kfc") || receiver.contains("pizza") || receiver.contains("eat") || receiver.contains("restaurant") -> "Dining Out"
                        receiver.contains("sacco") || receiver.contains("chama") || receiver.contains("saving") -> "Savings & Investments"
                        else -> "Other Organizations"
                    }
                }
                .mapValues { (_, txs) -> txs.sumOf { it.amount } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    
    fun updateDateRange(start: Long, end: Long) {
        _dateRange.value = Pair(start, end)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val spendingData by viewModel.categorySpending.collectAsState()
    val monthlyTrend by viewModel.monthlyTrend.collectAsState()
    val incomeSources by viewModel.incomeSources.collectAsState()
    val expenseGroups by viewModel.topExpenseGroups.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedReportType by remember { mutableStateOf("Category") }
    val isDark = isSystemInDarkTheme()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Advanced Analytics") },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf("Category", "Groups", "Trend", "Sources").forEachIndexed { index, label ->
                        SegmentedButton(
                            selected = selectedReportType == label,
                            onClick = { selectedReportType = label },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 4)
                        ) {
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            
            when (selectedReportType) {
                "Category" -> {
                    item {
                        Card {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Spending Breakdown", fontWeight = FontWeight.Bold)
                                if (spendingData.isNotEmpty()) {
                                    SpendingBarChart(spendingData, isDark)
                                } else {
                                    Text("No spending recorded in this period.", modifier = Modifier.padding(16.dp), color = Color.Gray)
                                }
                            }
                        }
                    }
                }
                "Groups" -> {
                    item {
                        Card {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Top Spending Areas", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(16.dp))
                                if (expenseGroups.isNotEmpty()) {
                                    expenseGroups.toList().sortedByDescending { it.second }.forEach { (group, amount) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = when(group) {
                                                        "Supermarkets" -> Icons.Default.ShoppingCart
                                                        "Utilities" -> Icons.Default.Power
                                                        "Subscriptions" -> Icons.Default.NotificationsActive
                                                        "Transport" -> Icons.Default.DirectionsCar
                                                        "Dining Out" -> Icons.Default.Restaurant
                                                        else -> Icons.Default.Business
                                                    },
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(group, style = MaterialTheme.typography.bodyLarge)
                                            }
                                            Text(
                                                "KSh ${String.format("%,.0f", amount)}",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                                    }
                                } else {
                                    Text("No spending data categorized yet.", color = Color.Gray)
                                }
                            }
                        }
                    }
                }
                "Trend" -> {
                    item {
                        Card {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Cash Flow (Income vs Expense)", fontWeight = FontWeight.Bold)
                                if (monthlyTrend.isNotEmpty()) {
                                    MonthlyTrendChart(monthlyTrend, isDark)
                                } else {
                                    Text("No trend data available.", modifier = Modifier.padding(16.dp), color = Color.Gray)
                                }
                            }
                        }
                    }
                }
                "Sources" -> {
                    item {
                        Card {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Income Sources", fontWeight = FontWeight.Bold)
                                if (incomeSources.isNotEmpty()) {
                                    IncomePieChart(incomeSources, isDark)
                                } else {
                                    Text("No income sources found.", modifier = Modifier.padding(16.dp), color = Color.Gray)
                                }
                            }
                        }
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
fun SpendingBarChart(data: Map<String, Double>, isDark: Boolean) {
    val textColor = if (isDark) android.graphics.Color.WHITE else android.graphics.Color.BLACK
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(300.dp),
        factory = { context ->
            BarChart(context).apply {
                description.isEnabled = false
                setDrawGridBackground(false)
                setDrawBarShadow(false)
                xAxis.textColor = textColor
                axisLeft.textColor = textColor
                axisRight.isEnabled = false
                legend.textColor = textColor
                animateY(1000)
            }
        },
        update = { chart ->
            val entries = data.values.mapIndexed { index, value -> BarEntry(index.toFloat(), value.toFloat()) }
            val dataSet = BarDataSet(entries, "Spending").apply {
                colors = ColorTemplate.MATERIAL_COLORS.toList()
                valueTextColor = textColor
                valueTextSize = 10f
            }
            chart.data = BarData(dataSet)
            chart.invalidate()
        }
    )
}

@Composable
fun MonthlyTrendChart(data: Map<String, Pair<Double, Double>>, isDark: Boolean) {
    val textColor = if (isDark) android.graphics.Color.WHITE else android.graphics.Color.BLACK
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(300.dp),
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                xAxis.textColor = textColor
                axisLeft.textColor = textColor
                axisRight.isEnabled = false
                legend.textColor = textColor
                setDrawGridBackground(false)
                animateX(1000)
            }
        },
        update = { chart ->
            val incomeEntries = data.values.mapIndexed { index, v -> Entry(index.toFloat(), v.first.toFloat()) }
            val expenseEntries = data.values.mapIndexed { index, v -> Entry(index.toFloat(), v.second.toFloat()) }
            
            val incomeSet = LineDataSet(incomeEntries, "Income").apply { 
                color = android.graphics.Color.GREEN
                setCircleColor(android.graphics.Color.GREEN)
                lineWidth = 3f
                valueTextColor = textColor
            }
            val expenseSet = LineDataSet(expenseEntries, "Expense").apply { 
                color = android.graphics.Color.RED
                setCircleColor(android.graphics.Color.RED)
                lineWidth = 3f
                valueTextColor = textColor
            }
            
            chart.data = LineData(incomeSet, expenseSet)
            chart.invalidate()
        }
    )
}

@Composable
fun IncomePieChart(data: Map<String, Double>, isDark: Boolean) {
    val textColor = if (isDark) android.graphics.Color.WHITE else android.graphics.Color.BLACK
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(300.dp),
        factory = { context ->
            PieChart(context).apply {
                description.isEnabled = false
                setUsePercentValues(true)
                setHoleColor(android.graphics.Color.TRANSPARENT)
                legend.textColor = textColor
                setEntryLabelColor(textColor)
                animateXY(1000, 1000)
            }
        },
        update = { chart ->
            val entries = data.map { PieEntry(it.value.toFloat(), it.key) }
            val dataSet = PieDataSet(entries, "Sources").apply {
                colors = ColorTemplate.VORDIPLOM_COLORS.toList()
                sliceSpace = 3f
                valueTextColor = textColor
                valueTextSize = 12f
            }
            chart.data = PieData(dataSet).apply {
                setValueFormatter(PercentFormatter())
            }
            chart.invalidate()
        }
    )
}
