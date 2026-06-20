package com.musafinance.pesamate.sms

import android.content.Context
import android.provider.Telephony
import com.musafinance.pesamate.data.local.TransactionEntity
import com.musafinance.pesamate.data.local.LoanEntity
import com.musafinance.pesamate.data.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsScanner @Inject constructor(
    private val repository: TransactionRepository
) {

    suspend fun scanHistoricalSms(context: Context, forceRescan: Boolean = false) = withContext(Dispatchers.IO) {
        android.util.Log.d("SmsScanner", "Starting deep PesaMate SMS scan...")
        
        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms._ID),
            null,
            null,
            Telephony.Sms.DATE + " DESC"
        )

        var scannedCount = 0
        var parsedCount = 0
        
        cursor?.use {
            val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
            val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)
            val idIndex = it.getColumnIndex(Telephony.Sms._ID)

            while (it.moveToNext()) {
                scannedCount++
                try {
                    val origin = it.getString(addressIndex) ?: "Unknown"
                    val body = it.getString(bodyIndex) ?: continue
                    val dateMillis = it.getLong(dateIndex)
                    val smsId = it.getLong(idIndex)

                    val parsed = SmsParser.parse(body, origin)
                    if (parsed != null) {
                        parsedCount++
                        
                        val uniqueId = parsed.transactionCode ?: "SMS_${smsId}"
                        
                        val entity = TransactionEntity(
                            id = uniqueId,
                            amount = parsed.amount,
                            date = dateMillis,
                            type = parsed.type.name,
                            category = parsed.category,
                            description = parsed.description,
                            transactionCode = parsed.transactionCode,
                            sender = parsed.sender,
                            receiver = parsed.receiver,
                            accountName = parsed.accountName,
                            balance = parsed.balance,
                            provider = parsed.provider,
                            isManual = false,
                            isSynced = false
                        )
                        repository.saveTransaction(entity)
                        
                        // Handle loan updates specifically
                        if (parsed.type == TransactionType.LOAN_DISBURSEMENT) {
                            val loanId = parsed.provider.replace(" ", "_")
                            val loanEntity = LoanEntity(
                                id = loanId,
                                lender = parsed.provider,
                                amountBorrowed = parsed.amount,
                                amountRepaid = 0.0,
                                outstandingBalance = parsed.balance ?: parsed.amount,
                                dueDate = dateMillis + (30L * 24 * 60 * 60 * 1000),
                                interestRate = 0.0,
                                isManual = false,
                                isSynced = false
                            )
                            repository.saveLoan(loanEntity)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SmsScanner", "Error processing SMS", e)
                }
            }
        }
        android.util.Log.d("SmsScanner", "PesaMate Scan complete. Scanned: $scannedCount, Parsed: $parsedCount")
    }
}
