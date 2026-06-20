package com.musafinance.pesamate.ui.subscriptions

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
import com.musafinance.pesamate.data.local.SubscriptionEntity
import com.musafinance.pesamate.data.repository.TransactionRepository
import com.musafinance.pesamate.sms.SmsScanner
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
    private val repository: TransactionRepository,
    private val smsScanner: SmsScanner
) : ViewModel() {

    val subscriptions: StateFlow<List<SubscriptionEntity>> = repository.allSubscriptions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun refresh(context: android.content.Context) {
        viewModelScope.launch {
            smsScanner.scanHistoricalSms(context)
        }
    }

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
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refresh(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subscriptions", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
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
                Text("KSh ${String.format(Locale.getDefault(), "%,.2f", sub.amount)}", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.error)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                }
            }
        }
    }
}
