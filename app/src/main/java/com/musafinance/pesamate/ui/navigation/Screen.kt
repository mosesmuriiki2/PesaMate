package com.musafinance.pesamate.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Transactions : Screen("transactions", "Transactions", Icons.AutoMirrored.Filled.List)
    object Expenses : Screen("expenses", "Expenses", Icons.Default.Payments)
    object Income : Screen("income", "Income", Icons.Default.ArrowDownward)
    object Savings : Screen("savings", "Savings", Icons.Default.Savings)
    object Loans : Screen("loans", "Loans", Icons.Default.AccountBalance)
    object Reports : Screen("reports", "Analytics", Icons.Default.Analytics)
    object Insights : Screen("insights", "Insights", Icons.Default.PieChart)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object AddTransaction : Screen("add_transaction", "Add", Icons.Default.Add)
    object AddLoan : Screen("add_loan", "Add Loan", Icons.Default.Add)
    object AddSubscription : Screen("add_subscription", "Add Sub", Icons.Default.Add)
    object AddSavings : Screen("add_savings", "Add Saving", Icons.Default.Add)
    object Budgets : Screen("budgets", "Budgets", Icons.Default.AccountBalanceWallet)
    object Subscriptions : Screen("subscriptions", "Subscriptions", Icons.Default.NotificationsActive)
    object PrivacyPolicy : Screen("privacy", "Privacy Policy", Icons.Default.Security)
    object TermsConditions : Screen("terms", "Terms & Conditions", Icons.Default.Gavel)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Transactions,
    Screen.Insights,
    Screen.Budgets
)

val drawerItems = listOf(
    Screen.Dashboard,
    Screen.Expenses,
    Screen.Savings,
    Screen.Subscriptions,
    Screen.Loans,
    Screen.Reports,
    Screen.Settings,
    Screen.PrivacyPolicy,
    Screen.TermsConditions
)
