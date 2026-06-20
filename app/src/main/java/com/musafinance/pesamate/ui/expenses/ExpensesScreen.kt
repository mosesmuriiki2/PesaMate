package com.musafinance.pesamate.ui.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musafinance.pesamate.data.local.TransactionEntity
import com.musafinance.pesamate.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    val expenses: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .map { list -> list.filter { it.type == "EXPENSE" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalExpenses: StateFlow<Double> = expenses
        .map { it.sumOf { tx -> tx.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val groupedExpenses: StateFlow<Map<String, List<TransactionEntity>>> = expenses
        .map { list -> 
            list.groupBy { tx ->
                val receiver = tx.receiver?.lowercase() ?: "other"
                when {
                    receiver.contains("supermarket") || receiver.contains("naivas") || receiver.contains("carrefour") -> "Groceries"
                    receiver.contains("kplc") || receiver.contains("power") || receiver.contains("water") -> "Utilities"
                    receiver.contains("uber") || receiver.contains("bolt") || receiver.contains("taxi") -> "Transport"
                    receiver.contains("java") || receiver.contains("kfc") || receiver.contains("pizza") -> "Dining Out"
                    else -> tx.category
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(viewModel: ExpensesViewModel = hiltViewModel()) {
    val groupedMap by viewModel.groupedExpenses.collectAsState()
    val total by viewModel.totalExpenses.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expense Breakdown", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Monthly Spending", color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f))
                    Text(
                        "KSh ${String.format("%,.2f", total)}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (groupedMap.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No expenses recorded yet.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    groupedMap.forEach { (category, txList) ->
                        item {
                            ExpenseGroupSection(category, txList)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseGroupSection(category: String, transactions: List<TransactionEntity>) {
    val totalForGroup = transactions.sumOf { it.amount }
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(category, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                Text("Total: KSh ${String.format("%,.0f", totalForGroup)}", fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            transactions.take(5).forEach { tx ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(tx.description, maxLines = 1, modifier = Modifier.weight(1f), fontSize = 14.sp)
                    Text("KSh ${String.format("%,.0f", tx.amount)}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
            if (transactions.size > 5) {
                Text("+ ${transactions.size - 5} more", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}
