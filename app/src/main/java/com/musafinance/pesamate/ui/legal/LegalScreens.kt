package com.musafinance.pesamate.ui.legal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Privacy Policy") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Privacy Policy", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Text("Last Updated: June 20, 2026", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(24.dp))
            
            LegalSection("1. Commitment to Privacy", "PesaMate is built with a 'Privacy-First' architecture. We understand that your financial data is highly sensitive and we are committed to protecting it through strictly local processing and state-of-the-art encryption.")
            
            LegalSection("2. On-Device Local Processing", "Unlike traditional finance apps, PesaMate performs all SMS scanning and transaction analysis directly on your smartphone. We do not transmit your raw SMS messages, personal conversations, or contact lists to any server.")
            
            LegalSection("3. Data We Collect", "To provide automated financial tracking, PesaMate requests access to your SMS messages. We selectively extract only financial metadata: transaction amounts, merchant/provider names, dates, and account identifiers. We do not store or process non-financial personal messages.")
            
            LegalSection("4. Optional Encrypted Cloud Sync", "If you choose to enable 'Cloud Sync' in Settings, PesaMate will upload your processed transaction data to a secure Google Firebase database. This data is encrypted in transit and at rest. This feature is disabled by default to ensure maximum privacy for local-only users.")
            
            LegalSection("5. Data Security (AES-256)", "Your local database is secured using industry-standard AES-256 encryption. We also support Biometric Authentication (Fingerprint/Facial Scan) to prevent unauthorized local access to your financial dashboard.")
            
            LegalSection("6. Compliance with Global Standards", "This policy is designed to comply with international regulations, including the GDPR (General Data Protection Regulation) and CCPA (California Consumer Privacy Act). You have the right to delete your data at any time via the app settings.")
            
            LegalSection("7. Third-Party Services", "PesaMate does not sell or lease your financial data to third-party advertisers. We have removed all ad-tracking SDKs (including AdMob) to maintain a clean, private experience for our users.")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsConditionsScreen() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Terms & Conditions") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Terms & Conditions", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Text("Last Updated: June 20, 2026", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(24.dp))
            
            LegalSection("1. Acceptance of Terms", "By accessing or using PesaMate, you agree to be bound by these Terms. If you do not agree, you must discontinue use immediately.")
            
            LegalSection("2. SMS Permission", "You grant PesaMate permission to read and analyze financial SMS notifications. You represent that you are the primary user of the device and have the legal right to access these notifications.")
            
            LegalSection("3. No Financial Advice", "The insights, charts, and reports provided by PesaMate are for informational and educational purposes only. They do not constitute professional financial, investment, or tax advice.")
            
            LegalSection("4. Disclaimer of Liability", "PesaMate is provided 'as-is' and 'as-available'. We are not responsible for inaccuracies caused by bank SMS formatting changes, device errors, or any financial losses incurred through the use of the app.")
            
            LegalSection("5. User Data Responsibility", "You are responsible for securing your device and managing your Biometric or PIN access. If you enable Cloud Sync, you are responsible for maintaining the security of your Google account.")
        }
    }
}

@Composable
fun LegalSection(title: String, content: String) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(title, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(6.dp))
        Text(content, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
