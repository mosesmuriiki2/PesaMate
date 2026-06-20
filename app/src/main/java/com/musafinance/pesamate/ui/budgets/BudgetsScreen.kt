package com.musafinance.pesamate.ui.budgets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import com.musafinance.pesamate.data.local.BudgetEntity
import com.musafinance.pesamate.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    val budgets: StateFlow<List<BudgetEntity>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addBudget(category: String, limit: Double) {
        viewModelScope.launch {
            val budget = BudgetEntity(
                id = UUID.randomUUID().toString(),
                category = category,
                limitAmount = limit,
                spentAmount = 0.0
            )
            repository.saveBudget(budget)
        }
    }

    fun deleteBudget(id: String) {
        viewModelScope.launch {
            repository.deleteBudget(id)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(viewModel: BudgetsViewModel = hiltViewModel()) {
    val budgets by viewModel.budgets.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budgets", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Budget")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (budgets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No budgets set. Tap + to add one.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(budgets) { budget ->
                        BudgetCard(budget, onDelete = { viewModel.deleteBudget(budget.id) })
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddBudgetDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { cat, limit ->
                viewModel.addBudget(cat, limit)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun BudgetCard(budget: BudgetEntity, onDelete: () -> Unit) {
    val progress = if (budget.limitAmount > 0) (budget.spentAmount / budget.limitAmount).toFloat() else 0f
    val color = when {
        progress > 0.9f -> Color.Red
        progress > 0.7f -> Color.Yellow
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(budget.category, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                }
            }
            Text("Limit: KSh ${String.format(Locale.getDefault(), "%,.0f", budget.limitAmount)}", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = color
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val spent = String.format(Locale.getDefault(), "%,.0f", budget.spentAmount)
                Text("Spent: KSh $spent", fontSize = 12.sp)
                Text("${(progress * 100).toInt()}%", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun AddBudgetDialog(onDismiss: () -> Unit, onConfirm: (String, Double) -> Unit) {
    var category by remember { mutableStateOf("") }
    var limit by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Budget") },
        text = {
            Column {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g. Shopping)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = limit,
                    onValueChange = { limit = it },
                    label = { Text("Limit (KSh)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val limitVal = limit.toDoubleOrNull() ?: 0.0
                if (category.isNotBlank() && limitVal > 0) {
                    onConfirm(category, limitVal)
                }
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
