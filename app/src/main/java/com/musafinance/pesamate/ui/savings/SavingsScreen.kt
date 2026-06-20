package com.musafinance.pesamate.ui.savings

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
import com.musafinance.pesamate.data.local.SavingsEntity
import com.musafinance.pesamate.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SavingsViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    val savings: StateFlow<List<SavingsEntity>> = repository.allSavings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalSavings: StateFlow<Double> = savings
        .map { it.sumOf { s -> s.balance } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun addManualSavings(name: String, amount: Double, provider: String) {
        viewModelScope.launch {
            val s = SavingsEntity(
                id = UUID.randomUUID().toString(),
                accountName = name,
                balance = amount,
                provider = provider,
                lastUpdated = System.currentTimeMillis()
            )
            repository.saveSavings(s)
        }
    }

    fun deleteSavings(id: String) {
        viewModelScope.launch {
            repository.deleteSavings(id)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsScreen(viewModel: SavingsViewModel = hiltViewModel()) {
    val savingsList by viewModel.savings.collectAsState()
    val totalSavings by viewModel.totalSavings.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Savings Accounts", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Savings")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Total Savings", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
                    Text(
                        "KSh ${String.format("%,.2f", totalSavings)}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("Your Savings Pots", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            if (savingsList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No savings accounts tracked yet.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(savingsList) { item ->
                        SavingsCard(item, onDelete = { viewModel.deleteSavings(item.id) })
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddSavingsDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, amount, provider ->
                viewModel.addManualSavings(name, amount, provider)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun SavingsCard(item: SavingsEntity, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.accountName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(item.provider, fontSize = 12.sp, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("KSh ${String.format("%,.2f", item.balance)}", fontWeight = FontWeight.ExtraBold, color = Color(0xFF2E7D32))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun AddSavingsDialog(onDismiss: () -> Unit, onConfirm: (String, Double, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var provider by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Savings Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Account Name (e.g. M-Shwari)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = provider, onValueChange = { provider = it }, label = { Text("Provider (e.g. Safaricom)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Current Balance") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amtVal = amount.toDoubleOrNull() ?: 0.0
                if (name.isNotBlank() && amtVal >= 0) {
                    onConfirm(name, amtVal, provider)
                }
            }) {
                Text("Add")
            }
        }
    )
}
