package com.musafinance.pesamate.ui.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musafinance.pesamate.data.repository.TransactionRepository
import com.musafinance.pesamate.ui.theme.ThemePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themePreferences: ThemePreferences,
    private val repository: TransactionRepository
) : ViewModel() {
    
    val isDarkMode: StateFlow<Boolean> = themePreferences.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    val isBiometricEnabled: StateFlow<Boolean> = themePreferences.isBiometricEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    val dailyLimit: StateFlow<Double> = themePreferences.dailyLimit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    
    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            themePreferences.toggleDarkMode(enabled)
        }
    }
    
    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch {
            themePreferences.toggleBiometric(enabled)
        }
    }
    
    fun setDailyLimit(limit: Double) {
        viewModelScope.launch {
            themePreferences.setDailyLimit(limit)
            repository.saveDailyLimit(limit)
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            repository.syncFromFirestore()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val dailyLimit by viewModel.dailyLimit.collectAsState()
    var showLimitDialog by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SettingsHeader("Appearance")
                SettingsCard {
                    SettingsToggle(
                        title = "Dark Mode",
                        subtitle = "Enable dark UI theme",
                        icon = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                        checked = isDarkMode,
                        onCheckedChange = { viewModel.toggleDarkMode(it) }
                    )
                }
            }

            item {
                SettingsHeader("Security & Privacy")
                SettingsCard {
                    Column {
                        SettingsToggle(
                            title = "Biometric Lock",
                            subtitle = "Secure PesaMate with fingerprint/face",
                            icon = Icons.Default.Fingerprint,
                            checked = isBiometricEnabled,
                            onCheckedChange = { viewModel.toggleBiometric(it) }
                        )
                        HorizontalDivider()
                        SettingsAction(
                            title = "Data Encryption",
                            subtitle = "PesaMate uses AES-256 for local storage",
                            icon = Icons.Default.VerifiedUser,
                            action = {}
                        )
                    }
                }
            }

            item {
                SettingsHeader("Data Management")
                SettingsCard {
                    Column {
                        SettingsAction(
                            title = "Restore from Cloud",
                            subtitle = "Pull data from your Firestore backup",
                            icon = Icons.Default.CloudSync,
                            action = { 
                                viewModel.triggerSync()
                                Toast.makeText(context, "Pulling data...", Toast.LENGTH_SHORT).show()
                            }
                        )
                        HorizontalDivider()
                        SettingsAction(
                            title = "Daily Spend Alert",
                            subtitle = if (dailyLimit > 0) "KSh ${String.format("%,.0f", dailyLimit)}" else "Not set",
                            icon = Icons.Default.Timer,
                            action = { showLimitDialog = true }
                        )
                    }
                }
            }

            item {
                SettingsHeader("App Info")
                SettingsCard {
                    Column {
                        SettingsAction(title = "App Version", subtitle = "1.0.0 (Global)", icon = Icons.Default.Info, action = {})
                        HorizontalDivider()
                        SettingsAction(title = "Legal", subtitle = "Privacy Policy & Terms", icon = Icons.Default.Description, action = {})
                    }
                }
            }
        }
    }

    if (showLimitDialog) {
        DailyLimitDialog(
            currentLimit = dailyLimit,
            onDismiss = { showLimitDialog = false },
            onSave = { limit ->
                viewModel.setDailyLimit(limit)
                showLimitDialog = false
            }
        )
    }
}

@Composable
fun SettingsHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 12.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        content()
    }
}

@Composable
fun SettingsToggle(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun SettingsAction(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, action: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        modifier = Modifier.clickable { action() },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun DailyLimitDialog(currentLimit: Double, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var limitText by remember { mutableStateOf(if (currentLimit > 0) currentLimit.toString() else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Daily Limit") },
        text = {
            OutlinedTextField(value = limitText, onValueChange = { limitText = it }, label = { Text("Amount (KSh)") }, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = {
            TextButton(onClick = { limitText.toDoubleOrNull()?.let { onSave(it) } }) { Text("Save") }
        }
    )
}
