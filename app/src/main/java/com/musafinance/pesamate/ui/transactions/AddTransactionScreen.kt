package com.musafinance.pesamate.ui.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musafinance.pesamate.data.local.TransactionEntity
import com.musafinance.pesamate.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.UUID

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {
    fun saveTransaction(amount: Double, type: String, category: String, description: String) {
        viewModelScope.launch {
            val tx = TransactionEntity(
                id = UUID.randomUUID().toString(),
                amount = amount,
                date = System.currentTimeMillis(),
                type = type,
                category = category,
                description = description,
                transactionCode = null,
                sender = null,
                receiver = null,
                accountName = null,
                balance = null,
                provider = "Manual",
                isManual = true
            )
            repository.saveTransaction(tx)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onDismiss: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food") }
    var type by remember { mutableStateOf("EXPENSE") }

    val categories = listOf("Food", "Transport", "Rent", "Utilities", "Salary", "Business", "Other")

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Add Transaction") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Text("Type")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = type == "EXPENSE",
                    onClick = { type = "EXPENSE" },
                    label = { Text("Expense") }
                )
                FilterChip(
                    selected = type == "INCOME",
                    onClick = { type = "INCOME" },
                    label = { Text("Income") }
                )
            }

            Text("Category")
            ScrollableTabRow(selectedTabIndex = categories.indexOf(category), edgePadding = 0.dp) {
                categories.forEach { cat ->
                    Tab(
                        selected = category == cat,
                        onClick = { category = cat },
                        text = { Text(cat) }
                    )
                }
            }

            Button(
                onClick = {
                    viewModel.saveTransaction(amount.toDoubleOrNull() ?: 0.0, type, category, description)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}