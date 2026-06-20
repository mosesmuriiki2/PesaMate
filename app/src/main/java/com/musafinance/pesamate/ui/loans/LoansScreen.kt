package com.musafinance.pesamate.ui.loans

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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.hilt.navigation.compose.hiltViewModel
import com.musafinance.pesamate.data.local.LoanEntity
import com.musafinance.pesamate.data.repository.TransactionRepository
import com.musafinance.pesamate.sms.SmsScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class LoansViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val smsScanner: SmsScanner
) : ViewModel() {
    
    val loans: StateFlow<List<LoanEntity>> = repository.allLoans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val loanOffers: StateFlow<List<LoanOffer>> = repository.allTransactions
        .map { transactions ->
            transactions
                .filter { it.type == "LOAN_OFFER" }
                .map { LoanOffer(it.provider, it.amount, it.description) }
                .take(5)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val totalOutstanding: StateFlow<Double> = loans
        .map { it.sumOf { loan -> loan.outstandingBalance } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun refreshLoans(context: android.content.Context) {
        viewModelScope.launch {
            smsScanner.scanHistoricalSms(context)
        }
    }
    
    fun addManualLoan(lender: String, amount: Double, interestRate: Double, dueDate: Long) {
        viewModelScope.launch {
            val loan = LoanEntity(
                id = UUID.randomUUID().toString(),
                lender = lender,
                amountBorrowed = amount,
                amountRepaid = 0.0,
                outstandingBalance = amount,
                dueDate = dueDate,
                interestRate = interestRate,
                isManual = true,
                isSynced = false
            )
            repository.saveLoan(loan)
        }
    }

    fun clearLoan(id: String) {
        viewModelScope.launch {
            repository.deleteLoan(id)
        }
    }
}

data class LoanOffer(
    val lender: String,
    val amount: Double,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(
    viewModel: LoansViewModel = hiltViewModel()
) {
    val loans by viewModel.loans.collectAsState()
    val loanOffers by viewModel.loanOffers.collectAsState()
    val totalOutstanding by viewModel.totalOutstanding.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Auto-refresh when entering screen
    LaunchedEffect(Unit) {
        viewModel.refreshLoans(context)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Loans & Credit") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Total Outstanding", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "KSh ${String.format(Locale.getDefault(), "%,.2f", totalOutstanding)}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            item { Text("Active Loans", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            
            if (loans.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No active loans detected", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
            
            items(loans) { loan ->
                LoanItem(loan, onClear = { viewModel.clearLoan(loan.id) })
            }
            
            if (loanOffers.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Available Loan Offers", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                items(loanOffers) { offer ->
                    LoanOfferItem(offer)
                }
            }
        }
    }
}

@Composable
fun LoanItem(loan: LoanEntity, onClear: () -> Unit) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val daysUntilDue = ((loan.dueDate - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (daysUntilDue < 7) MaterialTheme.colorScheme.errorContainer 
                          else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(loan.lender, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Due: ${dateFormat.format(Date(loan.dueDate))}", style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Clear", tint = Color(0xFF2E7D32))
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "KSh ${String.format(Locale.getDefault(), "%,.2f", loan.outstandingBalance)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Text("${loan.interestRate}% interest", style = MaterialTheme.typography.bodySmall)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            val progress = (loan.amountRepaid / loan.amountBorrowed.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            
            if (daysUntilDue < 7) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (daysUntilDue <= 0) "OVERDUE" else "Due in $daysUntilDue days", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LoanOfferItem(offer: LoanOffer) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(offer.lender, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(offer.description, style = MaterialTheme.typography.bodySmall)
            }
            Text("KSh ${String.format(Locale.getDefault(), "%,.2f", offer.amount)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoanDialog(onDismiss: () -> Unit, onAdd: (String, Double, Double, Long) -> Unit) {
    var lender by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var interestRate by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<Long>(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Manual Loan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = lender, onValueChange = { lender = it }, label = { Text("Lender") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = interestRate, onValueChange = { interestRate = it }, label = { Text("Interest %") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amt = amount.toDoubleOrNull() ?: 0.0
                val rate = interestRate.toDoubleOrNull() ?: 0.0
                if (lender.isNotBlank() && amt > 0) onAdd(lender, amt, rate, selectedDate)
            }) { Text("Add") }
        }
    )
}
