# PesaMate - Your Global Financial Companion

PesaMate is a professional, automated financial tracking application designed for the global market. It leverages on-device machine learning and advanced parsing algorithms to monitor your finances through SMS notifications from banks, mobile money providers (like M-Pesa, M-Shwari, KCB M-Pesa), and loan applications.

## 🚀 Key Features

*   **Automated SMS Tracking**: Automatically parses transaction data from various financial institutions globally.
*   **Global Support**: Intelligent recognition of multiple currencies ($, £, €, ¥, Ksh, etc.) and English-language financial keywords.
*   **Loan Management**: Track disbursements, repayments, and overdue balances from various lenders and banks (KCB, Equity, Tala, Branch, etc.).
*   **Savings Monitoring**: Automatically updates balances for savings accounts like M-Shwari and KCB M-Pesa based on deposit/withdrawal messages.
*   **Subscription Tracking**: Detects and monitors recurring payments for services like Netflix, Spotify, and Google.
*   **Advanced Analytics**: Visualizes your financial health with interactive charts for cash flow trends, income sources, and spending categories.
*   **Budgeting**: Set and monitor daily or monthly spending limits with real-time progress tracking.
*   **Secure & Private**: Features biometric (fingerprint/face) unlock and AES-256 encryption for local data storage.
*   **Cloud Sync**: Optional encrypted backup to Google Firebase for multi-device data restoration.

## 🛠 Tech Stack

*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose (Modern, Dynamic, Animated)
*   **Database**: Room (Offline-first architecture)
*   **Dependency Injection**: Hilt
*   **Background Tasks**: WorkManager
*   **Analytics & Backup**: Firebase Firestore, Auth, Messaging
*   **Security**: BiometricPrompt, Security Crypto (AES-256)
*   **Monetization**: AdMob Integration

## 🔒 Privacy

PesaMate is built with a "local-first" principle. All SMS processing happens on your device. We do not read personal conversations, only financial notifications.

---
Developed by **mosesmuriiki2**