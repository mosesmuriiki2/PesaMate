package com.musafinance.pesamate.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.musafinance.pesamate.data.repository.TransactionRepository
import com.musafinance.pesamate.sync.SyncManager
import com.musafinance.pesamate.ui.navigation.*
import com.musafinance.pesamate.ui.theme.PesaMateTheme
import com.musafinance.pesamate.ui.theme.ThemePreferences
import com.musafinance.pesamate.notifications.NotificationHelper
import com.musafinance.pesamate.ui.onboarding.OnboardingScreen
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val syncManager: SyncManager,
    private val themePreferences: ThemePreferences,
    private val repository: TransactionRepository
) : ViewModel() {
    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()
    
    val isDarkMode: StateFlow<Boolean> = themePreferences.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val hasSeenOnboarding: StateFlow<Boolean> = themePreferences.hasSeenOnboarding
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isCloudSyncEnabled: StateFlow<Boolean> = themePreferences.isCloudSyncEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun startSync() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                repository.syncFromFirestore()
                syncManager.triggerImmediateSync()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error during initial sync", e)
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            themePreferences.setHasSeenOnboarding(true)
        }
    }

    fun toggleCloudSync(enabled: Boolean) {
        viewModelScope.launch {
            themePreferences.toggleCloudSync(enabled)
        }
    }
}

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private var isAuthenticated by mutableStateOf(false)
    private var showSplash by mutableStateOf(true)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            mainViewModel.startSync()
        } else {
            Toast.makeText(this, "SMS & Notification permissions are required.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createNotificationChannels(this)
        
        lifecycleScope.launch {
            delay(2000) // Show splash for 2s
            showSplash = false
            showBiometricPrompt()
        }

        setContent {
            val isDarkMode by mainViewModel.isDarkMode.collectAsState()
            val hasSeenOnboarding by mainViewModel.hasSeenOnboarding.collectAsState()
            
            PesaMateTheme(darkTheme = isDarkMode) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    when {
                        showSplash -> SplashScreen()
                        !hasSeenOnboarding -> OnboardingScreen { mainViewModel.completeOnboarding() }
                        !isAuthenticated -> AuthenticationOverlay { showBiometricPrompt() }
                        else -> MainContent()
                    }
                }
            }
        }

        checkAndRequestPermissions()
    }

    @Composable
    fun SplashScreen() {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("PesaMate", style = MaterialTheme.typography.headlineLarge)
                Text("Your Global Finance Companion", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainContent() {
        val navController = rememberNavController()
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        val currentRoute = currentDestination?.route
        
        var showPrivacyDisclosure by remember { mutableStateOf(false) }
        val hasSeenOnboarding by mainViewModel.hasSeenOnboarding.collectAsState()
        
        // Show disclosure once after onboarding
        LaunchedEffect(hasSeenOnboarding) {
            if (hasSeenOnboarding) {
                showPrivacyDisclosure = true
            }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("PesaMate", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    HorizontalDivider()
                    drawerItems.forEach { screen ->
                        NavigationDrawerItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                    
                    HorizontalDivider()
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Share, contentDescription = null) },
                        label = { Text("Share PesaMate") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Track your finances globally with PesaMate! Download now on Play Store.")
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            startActivity(shareIntent)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        ) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("PesaMate") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                    )
                },
                bottomBar = { PesaMateBottomBar(navController) },
                floatingActionButton = {
                    DynamicFAB(currentRoute, navController)
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    AppNavigation(navController)
                }
            }
        }
        
        if (showPrivacyDisclosure) {
            PrivacyDisclosureDialog(
                onDismiss = { showPrivacyDisclosure = false },
                onEnableCloud = { 
                    mainViewModel.toggleCloudSync(true)
                    showPrivacyDisclosure = false
                }
            )
        }
    }

    @Composable
    fun DynamicFAB(currentRoute: String?, navController: androidx.navigation.NavController) {
        val fabIcon = Icons.Default.Add
        
        val fabLabel = when (currentRoute) {
            Screen.Loans.route -> "Add Loan"
            Screen.Subscriptions.route -> "Add Sub"
            Screen.Savings.route -> "Add Saving"
            else -> "Add Tx"
        }

        val showFAB = currentRoute in listOf(
            Screen.Dashboard.route,
            Screen.Transactions.route,
            Screen.Loans.route,
            Screen.Subscriptions.route,
            Screen.Savings.route
        )

        if (showFAB) {
            ExtendedFloatingActionButton(
                onClick = {
                    when (currentRoute) {
                        Screen.Loans.route -> navController.navigate(Screen.AddLoan.route)
                        Screen.Subscriptions.route -> navController.navigate(Screen.AddSubscription.route)
                        Screen.Savings.route -> navController.navigate(Screen.AddSavings.route)
                        else -> navController.navigate(Screen.AddTransaction.route)
                    }
                },
                icon = { Icon(fabIcon, contentDescription = null) },
                text = { Text(fabLabel) }
            )
        }
    }

    @Composable
    fun AuthenticationOverlay(onRetry: () -> Unit) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Authentication Required", style = MaterialTheme.typography.titleLarge)
                Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Unlock PesaMate")
                }
            }
        }
    }

    @Composable
    fun PrivacyDisclosureDialog(onDismiss: () -> Unit, onEnableCloud: () -> Unit) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Privacy & Data Security") },
            text = {
                Column {
                    Text("PesaMate values your privacy. We want you to know how your data is handled:")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("• SMS processing is done strictly on your device.", fontWeight = FontWeight.Bold)
                    Text("• We do not read personal conversations.", fontWeight = FontWeight.Bold)
                    Text("• Your data is encrypted using AES-256 standards.")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Would you like to enable encrypted cloud backup to restore your data on other devices? (Disabled by default)")
                }
            },
            confirmButton = {
                Button(onClick = onEnableCloud) {
                    Text("Enable Backup")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Keep Local Only")
                }
            }
        )
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    isAuthenticated = true
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("PesaMate Unlock")
            .setSubtitle("Authenticate to access your dashboard")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            mainViewModel.startSync()
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
        }
    }
}
