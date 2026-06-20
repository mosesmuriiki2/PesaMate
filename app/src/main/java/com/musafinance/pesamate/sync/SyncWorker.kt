package com.musafinance.pesamate.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.musafinance.pesamate.data.repository.TransactionRepository
import com.musafinance.pesamate.sms.SmsScanner
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: TransactionRepository,
    private val smsScanner: SmsScanner
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("SyncWorker", "Starting PesaMate sync background work...")
            
            // 1. Fetch data FROM Firestore first (pull cloud data to local)
            repository.syncFromFirestore()
            
            // 2. Scan for new SMS transactions (local data updated)
            smsScanner.scanHistoricalSms(appContext)
            
            // 3. Upload any unsynced local transactions to Firestore (backed up)
            repository.syncUnsyncedData()

            // 4. Check for loan deadlines and notify user
            repository.checkLoanDeadlines()
            
            Log.d("SyncWorker", "Sync work completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync process encountered an error", e)
            Result.retry()
        }
    }
}
