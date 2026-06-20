package com.musafinance.pesamate.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.musafinance.pesamate.R
import com.musafinance.pesamate.ui.MainActivity

object NotificationHelper {
    
    private const val CHANNEL_ID_SPENDING = "spending_alerts"
    private const val CHANNEL_ID_LOANS = "loan_reminders"
    private const val CHANNEL_ID_GENERAL = "general_notifications"
    
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val spendingChannel = NotificationChannel(
                CHANNEL_ID_SPENDING,
                "Spending Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for daily spending limits"
            }
            
            val loanChannel = NotificationChannel(
                CHANNEL_ID_LOANS,
                "Loan Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for loan due dates"
            }
            
            val generalChannel = NotificationChannel(
                CHANNEL_ID_GENERAL,
                "General",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General app notifications"
            }
            
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(spendingChannel)
            manager.createNotificationChannel(loanChannel)
            manager.createNotificationChannel(generalChannel)
        }
    }
    
    fun showDailyLimitNotification(
        context: Context,
        spentAmount: Double,
        limitAmount: Double,
        percentageUsed: Int
    ) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val title = when {
            percentageUsed >= 100 -> "⚠️ Daily Limit Exceeded!"
            percentageUsed >= 90 -> "⚠️ Daily Limit Almost Reached!"
            percentageUsed >= 75 -> "Daily Spending Alert"
            else -> "Spending Update"
        }
        
        val message = "You've spent KSh ${String.format("%,.2f", spentAmount)} " +
                "of your KSh ${String.format("%,.2f", limitAmount)} daily limit ($percentageUsed%)"
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SPENDING)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        with(NotificationManagerCompat.from(context)) {
            try {
                notify(1001, notification)
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }
    
    fun showLoanDueNotification(
        context: Context,
        lenderName: String,
        amount: Double,
        daysUntilDue: Int
    ) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val title = when {
            daysUntilDue <= 0 -> "Loan Payment Overdue!"
            daysUntilDue <= 3 -> "Loan Due Soon!"
            else -> "Loan Reminder"
        }
        
        val message = "Your loan from $lenderName (KSh ${String.format("%,.2f", amount)}) is " +
                if (daysUntilDue <= 0) "overdue" else "due in $daysUntilDue days"
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_LOANS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        with(NotificationManagerCompat.from(context)) {
            try {
                notify(2000 + lenderName.hashCode(), notification)
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }
    
    fun showTransactionNotification(
        context: Context,
        title: String,
        message: String
    ) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_GENERAL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        with(NotificationManagerCompat.from(context)) {
            try {
                notify(System.currentTimeMillis().toInt(), notification)
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }
}
