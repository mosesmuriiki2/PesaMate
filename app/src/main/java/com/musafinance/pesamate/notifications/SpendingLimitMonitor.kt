package com.musafinance.pesamate.notifications

import android.content.Context
import com.musafinance.pesamate.data.local.PesaMateDao
import com.musafinance.pesamate.ui.theme.ThemePreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpendingLimitMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: PesaMateDao,
    private val preferences: ThemePreferences
) {
    
    suspend fun checkDailyLimit() {
        val limit = dao.getDailyLimit()
        if (limit == null || limit.limitAmount <= 0) return
        
        val now = Calendar.getInstance()
        val todayStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(now.time)
        
        val startOfDay = now.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val spentToday = dao.getTotalSpendingForDay(startOfDay) ?: 0.0
        val percentageUsed = ((spentToday / limit.limitAmount) * 100).toInt()
        
        val lastThreshold = preferences.lastNotifiedThreshold.first()
        val lastDate = preferences.lastNotifiedDate.first()
        
        android.util.Log.d("SpendingLimitMonitor", "Spent today: $spentToday, Limit: ${limit.limitAmount}, %: $percentageUsed, LastNotified: $lastThreshold on $lastDate")

        val currentThreshold = when {
            percentageUsed >= 100 -> 100
            percentageUsed >= 90 -> 90
            percentageUsed >= 75 -> 75
            else -> 0
        }

        if (currentThreshold > 0 && (currentThreshold > lastThreshold || todayStr != lastDate)) {
            NotificationHelper.showDailyLimitNotification(
                context,
                spentToday,
                limit.limitAmount,
                percentageUsed
            )
            preferences.setLastNotified(currentThreshold, todayStr)
        }
    }
    
    fun monitorInBackground() {
        CoroutineScope(Dispatchers.IO).launch {
            checkDailyLimit()
        }
    }
}
