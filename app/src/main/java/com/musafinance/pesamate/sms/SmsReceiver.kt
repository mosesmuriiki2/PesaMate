package com.musafinance.pesamate.sms


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.widget.Toast
import com.musafinance.pesamate.data.local.TransactionEntity
import com.musafinance.pesamate.data.repository.TransactionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: TransactionRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (msg in messages) {
                val origin = msg.displayOriginatingAddress ?: continue
                val body = msg.displayMessageBody ?: continue

                // Apply parsing engine rules
                val parsed = SmsParser.parse(body, origin)
                if (parsed != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val entity = TransactionEntity(
                            id = parsed.transactionCode ?: java.util.UUID.randomUUID().toString(),
                            amount = parsed.amount,
                            date = parsed.date.time,
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
                    }
                    Toast.makeText(context, "PesaMate parsed transaction code: ${parsed.transactionCode}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}