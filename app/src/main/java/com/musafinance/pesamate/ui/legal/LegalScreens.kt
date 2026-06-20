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
            Text("Privacy Policy", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            LegalSection("Overview", "PesaMate (\"we\", \"us\", or \"our\") is committed to protecting your privacy. This Privacy Policy explains how we collect, use, and safeguard your financial information when you use our mobile application.")
            
            LegalSection("Data Processing (Local-First)", "PesaMate operates on a 'local-first' principle. Your SMS financial notifications are scanned and processed locally on your device using on-device machine learning and regex algorithms. We do not transmit your raw SMS messages to our servers.")
            
            LegalSection("Information We Collect", "To provide financial tracking, we extract transaction amounts, merchant names, categories, and dates from SMS messages sent by financial institutions (Banks, Mobile Money, Loan Apps). We do not read personal or non-financial messages.")
            
            LegalSection("Data Security", "We implement industry-standard security measures, including AES-256 encryption for local data storage and secure TLS connections for encrypted cloud backups (if enabled by you).")
            
            LegalSection("User Rights & Transparency", "You have full control over your data. You can delete your local database or your cloud backup at any time within the app settings. PesaMate does not sell or share your financial data with third-party advertisers.")
            
            LegalSection("Compliance", "This policy is designed to comply with global data protection standards, including GDPR and CCPA, ensuring transparency and user agency.")
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
            Text("Terms & Conditions", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            LegalSection("Acceptance of Terms", "By downloading or using PesaMate, you agree to these Terms and Conditions. If you disagree with any part of these terms, you must immediately stop using the application.")
            
            LegalSection("Grant of Permissions", "The app requires READ_SMS and RECEIVE_SMS permissions to function. You acknowledge that providing these permissions allows PesaMate to analyze your financial notifications for the purpose of personal finance management.")
            
            LegalSection("No Financial Advice", "PesaMate is a data visualization and tracking tool. It does not provide professional financial, investment, or legal advice. All financial decisions are your sole responsibility.")
            
            LegalSection("Limitation of Liability", "PesaMate and its developers are not liable for any financial inaccuracies, bank errors, missed loan deadlines, or losses resulting from the use of the app. The automated tracking is provided 'as-is'.")
            
            LegalSection("Subscription Services", "If you use PesaMate to track recurring payments, you acknowledge that the app is not responsible for making actual payments or managing your subscriptions with third-party vendors.")
        }
    }
}

@Composable
fun LegalSection(title: String, content: String) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(title, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(content, style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
