package com.musafinance.pesamate.ui.subscriptions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
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
import com.musafinance.pesamate.data.local.SubscriptionEntity
import com.musafinance.pesamate.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    val subscriptions: StateFlow<List<SubscriptionEntity>> = repository.allSubscriptions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addManualSubscription(name: String, amount: Double, frequency: String, nextDate: Long) {
        viewModelScope.launch {
            val sub = SubscriptionEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                amount = amount,
                category = "Subscription",
                nextBillingDate = nextDate,
                frequency = frequency,
                isAutoTracked = false
            )
            repository.saveSubscription(sub)
        }
    }

    fun deleteSubscription(subscription: SubscriptionEntity) {
        viewModelScope.launch {
            repository.deleteSubscription(subscription)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(viewModel: SubscriptionsViewModel = hiltViewModel()) {
    val subscriptions by viewModel.subscriptions.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subscriptions", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Subscription")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (subscriptions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                        Text("No active subscriptions found.", color = Color.Gray)
                        Text("PesaMate tracks recurring payments automatically.", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(subscriptions) { sub ->
                        SubscriptionCard(sub, onDelete = { viewModel.deleteSubscription(sub) })
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddSubscriptionDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, amount, freq, date ->
                viewModel.addManualSubscription(name, amount, freq, date)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun SubscriptionCard(sub: SubscriptionEntity, onDelete: () -> Unit) {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(sub.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Next Billing: ${sdf.format(Date(sub.nextBillingDate))}", fontSize = 14.sp)
                Text("Frequency: ${sub.frequency}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("KSh ${String.format(Locale.getDefault(), "%,.2f", sub.amount)}", fontWeight = FontWeight.ExtraBold, color = Color.Red)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun AddSubscriptionDialog(onDismiss: () -> Unit, onConfirm: (String, Double, String, Long) -> Unit) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("Monthly") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Subscription") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Service Name (e.g. Netflix)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (KSh)") }, modifier = Modifier.fillMaxWidth())
                
                Text("Frequency", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Weekly", "Monthly", "Yearly").forEach { freq ->
                        FilterChip(
                            selected = frequency == freq,
                            onClick = { frequency = freq },
                            label = { Text(freq) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amtVal = amount.toDoubleOrNull() ?: 0.0
                if (name.isNotBlank() && amtVal > 0) {
                    onConfirm(name, amtVal, frequency, System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000))
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
