package com.musafinance.pesamate.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.musafinance.pesamate.ui.dashboard.DashboardScreen
import com.musafinance.pesamate.ui.transactions.TransactionsScreen
import com.musafinance.pesamate.ui.transactions.AddTransactionScreen
import com.musafinance.pesamate.ui.reports.ReportsScreen
import com.musafinance.pesamate.ui.settings.SettingsScreen
import com.musafinance.pesamate.ui.loans.LoansScreen
import com.musafinance.pesamate.ui.loans.AddLoanScreen
import com.musafinance.pesamate.ui.insights.InsightsScreen
import com.musafinance.pesamate.ui.budgets.BudgetsScreen
import com.musafinance.pesamate.ui.subscriptions.SubscriptionsScreen
import com.musafinance.pesamate.ui.subscriptions.AddSubscriptionScreen
import com.musafinance.pesamate.ui.expenses.ExpensesScreen
import com.musafinance.pesamate.ui.savings.SavingsScreen
import com.musafinance.pesamate.ui.savings.AddSavingsScreen
import com.musafinance.pesamate.ui.legal.PrivacyPolicyScreen
import com.musafinance.pesamate.ui.legal.TermsConditionsScreen

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(onNavigateToExpenses = { 
                navController.navigate(Screen.AddTransaction.route)
            })
        }
        composable(Screen.Transactions.route) {
            TransactionsScreen()
        }
        composable(Screen.AddTransaction.route) {
            AddTransactionScreen(onDismiss = { navController.popBackStack() })
        }
        composable(Screen.Loans.route) {
            LoansScreen()
        }
        composable(Screen.AddLoan.route) {
            AddLoanScreen(onDismiss = { navController.popBackStack() })
        }
        composable(Screen.Insights.route) {
            InsightsScreen()
        }
        composable(Screen.Reports.route) {
            ReportsScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
        composable(Screen.Budgets.route) {
            BudgetsScreen()
        }
        composable(Screen.Subscriptions.route) {
            SubscriptionsScreen()
        }
        composable(Screen.AddSubscription.route) {
            AddSubscriptionScreen(onDismiss = { navController.popBackStack() })
        }
        composable(Screen.Expenses.route) {
            ExpensesScreen()
        }
        composable(Screen.Savings.route) {
            SavingsScreen()
        }
        composable(Screen.AddSavings.route) {
            AddSavingsScreen(onDismiss = { navController.popBackStack() })
        }
        composable(Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen()
        }
        composable(Screen.TermsConditions.route) {
            TermsConditionsScreen()
        }
    }
}
