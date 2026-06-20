package com.musafinance.pesamate.sms

import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

enum class TransactionType {
    INCOME, EXPENSE, LOAN_DISBURSEMENT, LOAN_REPAYMENT, SAVINGS, TRANSFER, LOAN_OFFER, LOAN_DUE, FULIZA_RECOVERY, OTHER
}

data class ParsedTransaction(
    val transactionCode: String?,
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val provider: String,
    val sender: String?,
    val receiver: String?,
    val accountName: String?,
    val balance: Double?,
    val date: Date,
    val description: String,
    val rawBody: String = ""
)

object SmsParser {

    // 1. M-Pesa Sent Money Pattern
    private val mpesaSentPattern = Pattern.compile(
        "(?i)([A-Z0-9]{10})\\s*Confirmed.*?\\s*(?:Ksh|KSh|KES)?\\s*([\\d,]+\\.\\d{2})\\s+(?:sent|transferred).*?\\s+to\\s+([^\\s]+(?:\\s+[^\\s]+)*?)\\s+on\\s+(\\d{1,2}/\\d{1,2}/\\d{2,4}).*?at\\s+(\\d{1,2}:\\d{2}\\s*[APMapm]{2}).*?balance.*?is\\s*(?:Ksh|KSh|KES)?\\s*([\\d,]+\\.\\d{2})"
    )
    
    // 2. M-Pesa Received Money Pattern (Highly flexible)
    private val mpesaReceivedPattern = Pattern.compile(
        "(?i)([A-Z0-9]{10})\\s*Confirmed.*?You have received\\s*(?:Ksh|KSh|KES)?\\s*([\\d,]+\\.\\d{2})\\s+from\\s+(.*?)\\s+on\\s+(\\d{1,2}/\\d{1,2}/\\d{2,4}).*?at\\s+(\\d{1,2}:\\d{2}\\s*[APMapm]{2}).*?balance.*?is\\s*(?:Ksh|KSh|KES)?\\s*([\\d,]+\\.\\d{2})"
    )

    // 3. Paybill / Merchant
    private val paybillPattern = Pattern.compile(
        "(?i)([A-Z0-9]{10})\\s*Confirmed.*?\\s*(?:Ksh|KSh|KES)?\\s*([\\d,]+\\.\\d{2})\\s+paid\\s+to\\s+(.*?)\\.\\s*on (\\d{1,2}/\\d{1,2}/\\d{2,4}).*?at\\s+(\\d{1,2}:\\d{2}\\s*[APMapm]{2}).*?balance.*?is\\s*(?:Ksh|KSh|KES)?\\s*([\\d,]+\\.\\d{2})"
    )

    // 4. Loan Disbursal (Banks, Apps)
    private val loanDisbursedPattern = Pattern.compile(
        "(?i)([A-Z0-9]{10})?\\s*(?:Confirmed|Dear|Hello).*?(?:borrowed|disbursed|approved|loan|advance|credit|offered).*?\\s*(?:Ksh|KES|\\$|£|€)?\\s*([\\d,]+\\.\\d{2}).*?(?:from|by)?\\s*([^\\s]+(?:\\s+[^\\s]+){0,2})"
    )
    
    // 5. Fuliza Loan
    private val fulizaLoanPattern = Pattern.compile(
        "(?i)([A-Z0-9]{10})\\s*Confirmed.*?Fuliza.*?amount is\\s*(?:Ksh|KSh|KES)?\\s*([\\d,]+\\.\\d{2}).*?outstanding.*?is\\s*(?:Ksh|KSh|KES)?\\s*([\\d,]+\\.\\d{2})"
    )

    // 6. Savings Deposit
    private val savingsDepositPattern = Pattern.compile(
        "(?i)([A-Z0-9]{10})\\s*Confirmed.*?\\s*(?:Ksh|KSh|KES)?\\s*([\\d,]+\\.\\d{2})\\s+(?:transferred|saved|deposited|sent)\\s+to\\s+(?:your\\s+)?(M-Shwari|KCB M-PESA|Lock|Savings|Bank|Account).*?balance.*?is\\s*(?:Ksh|KSh|KES)?\\s*([\\d,]+\\.\\d{2})"
    )

    // 7. Savings Withdrawal
    private val savingsWithdrawalPattern = Pattern.compile(
        "(?i)([A-Z0-9]{10})\\s*Confirmed.*?transferr?ed|withdrawn\\s*(?:Ksh|KSh|KES)?\\s*([\\d,]+\\.\\d{2})\\s+from\\s+your\\s+(.*?)\\s+account.*?balance.*?is\\s*(?:Ksh|KSh|KES)?\\s*([\\d,]+\\.\\d{2})"
    )

    // 8. Global Generic Transaction
    private val globalTransactionPattern = Pattern.compile(
        "(?i)(?:paid|received|spent|sent|credited|debited|transfer|payment|withdrawal|purchase|bought).*?\\s*(?:Ksh|KES|\\$|£|€|¥|Rs|R|AED)?\\s*([\\d,]+\\.\\d{2})\\s*(?:to|from|at|by)?\\s*([^\\s]+(?:\\s+[^\\s]+)*?)\\s*(?:on|at|date)?"
    )

    // 9. Subscription Detection
    private val subscriptionPattern = Pattern.compile(
        "(?i)(netflix|spotify|youtube|apple|google|disney|showmax|prime|sub|renew|membership).*?\\s*(?:Ksh|KES|\\$|£|€|¥)?\\s*([\\d,]+\\.\\d{2})"
    )

    // 10. Overdue Loan Check (Banks: KCB, Equity, etc.)
    private val overdueLoanPattern = Pattern.compile(
        "(?i)(?:Dear|Hello).*?your\\s+(.*?)\\s+loan.*?overdue.*?([\\d,]+(?:\\.\\d{2})?)"
    )
    
    // 10b. Secondary Overdue Pattern (Amount comes later)
    private val overdueLoanPattern2 = Pattern.compile(
        "(?i)(?:Dear|Hello).*?your\\s+(.*?)\\s+loan.*?overdue.*?Ksh\\.?\\s*([\\d,]+(?:\\.\\d{2})?)"
    )

    // 11. Failed Transaction / Global Pay (Insufficient Funds)
    private val failedTransactionPattern = Pattern.compile(
        "(?i)unable to process.*?transaction on\\s+(.*?)\\s+(?:due to insufficient funds|due to)"
    )

    fun parse(smsBody: String, smsSender: String): ParsedTransaction? {
        val cleanBody = smsBody.replace("\n", " ").trim()
        val defaultDate = Date()

        try {
            // 1. Overdue Loan Check
            var matcher = overdueLoanPattern.matcher(cleanBody)
            if (!matcher.find()) matcher = overdueLoanPattern2.matcher(cleanBody)
            if (matcher.find()) {
                val type = matcher.group(1)?.trim() ?: "Loan"
                val amount = matcher.group(2)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                return ParsedTransaction(
                    transactionCode = "DUE_" + System.currentTimeMillis().toString().takeLast(6),
                    amount = amount,
                    type = TransactionType.LOAN_DUE,
                    category = "Loan Overdue",
                    provider = smsSender,
                    sender = smsSender,
                    receiver = "My Wallet",
                    accountName = type,
                    balance = amount,
                    date = defaultDate,
                    description = "$type loan is OVERDUE",
                    rawBody = cleanBody
                )
            }

            // 2. Savings Withdrawal Check
            matcher = savingsWithdrawalPattern.matcher(cleanBody)
            if (matcher.find()) {
                val code = matcher.group(1) ?: "WIT_${System.currentTimeMillis().toString().takeLast(6)}"
                val amount = matcher.group(2)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                val account = matcher.group(3)?.trim() ?: "Savings"
                val balance = matcher.group(4)?.replace(",", "")?.toDoubleOrNull()
                
                return ParsedTransaction(
                    transactionCode = code,
                    amount = amount,
                    type = TransactionType.TRANSFER,
                    category = "Savings Withdrawal",
                    provider = account,
                    sender = account,
                    receiver = "My Wallet",
                    accountName = account,
                    balance = balance,
                    date = defaultDate,
                    description = "Withdrawal from $account",
                    rawBody = cleanBody
                )
            }

            // 3. Savings Deposit Check
            matcher = savingsDepositPattern.matcher(cleanBody)
            if (matcher.find()) {
                val code = matcher.group(1) ?: "DEP_${System.currentTimeMillis().toString().takeLast(6)}"
                val amount = matcher.group(2)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                val account = matcher.group(3)?.trim() ?: "Savings"
                val balance = matcher.group(4)?.replace(",", "")?.toDoubleOrNull()
                
                return ParsedTransaction(
                    transactionCode = code,
                    amount = amount,
                    type = TransactionType.SAVINGS,
                    category = "Savings Deposit",
                    provider = account,
                    sender = "My Wallet",
                    receiver = account,
                    accountName = account,
                    balance = balance,
                    date = defaultDate,
                    description = "Deposit to $account",
                    rawBody = cleanBody
                )
            }

            // 4. Failed Transaction (Subscription Detection)
            matcher = failedTransactionPattern.matcher(cleanBody)
            if (matcher.find()) {
                val merchant = matcher.group(1)?.trim() ?: "Service"
                return ParsedTransaction(
                    transactionCode = "FAIL_" + System.currentTimeMillis().toString().takeLast(6),
                    amount = 0.0,
                    type = TransactionType.OTHER,
                    category = "Subscription",
                    provider = merchant,
                    sender = "My Wallet",
                    receiver = merchant,
                    accountName = null,
                    balance = null,
                    date = defaultDate,
                    description = "Declined: $merchant",
                    rawBody = cleanBody
                )
            }

            // 5. Subscription Check
            matcher = subscriptionPattern.matcher(cleanBody)
            if (matcher.find()) {
                val service = matcher.group(1)?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } ?: "Subscription"
                val amount = matcher.group(2)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                
                return ParsedTransaction(
                    transactionCode = "SUB_" + System.currentTimeMillis().toString().takeLast(6),
                    amount = amount,
                    type = TransactionType.EXPENSE,
                    category = "Subscription",
                    provider = service,
                    sender = "My Wallet",
                    receiver = service,
                    accountName = null,
                    balance = null,
                    date = defaultDate,
                    description = "$service payment",
                    rawBody = cleanBody
                )
            }

            // 6. Loan Disbursed Check
            matcher = loanDisbursedPattern.matcher(cleanBody)
            if (matcher.find()) {
                val amount = matcher.group(2)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                val lender = matcher.group(3)?.trim() ?: smsSender
                
                return ParsedTransaction(
                    transactionCode = matcher.group(1) ?: "LOAN_${System.currentTimeMillis().toString().takeLast(6)}",
                    amount = amount,
                    type = TransactionType.LOAN_DISBURSEMENT,
                    category = "Loan",
                    provider = lender,
                    sender = lender,
                    receiver = "My Wallet",
                    accountName = null,
                    balance = null,
                    date = defaultDate,
                    description = "Loan from $lender",
                    rawBody = cleanBody
                )
            }

            // 7. M-Pesa Received
            matcher = mpesaReceivedPattern.matcher(cleanBody)
            if (matcher.find()) {
                val code = matcher.group(1)
                val amount = matcher.group(2)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                val sender = matcher.group(3)?.trim() ?: "Unknown"
                val balance = matcher.group(6)?.replace(",", "")?.toDoubleOrNull()
                
                return ParsedTransaction(
                    transactionCode = code,
                    amount = amount,
                    type = TransactionType.INCOME,
                    category = "Transfer",
                    provider = "M-Pesa",
                    sender = sender,
                    receiver = "My Wallet",
                    accountName = null,
                    balance = balance,
                    date = defaultDate,
                    description = "Received from $sender",
                    rawBody = cleanBody
                )
            }
            
            // 8. M-Pesa Sent
            matcher = mpesaSentPattern.matcher(cleanBody)
            if (matcher.find()) {
                val code = matcher.group(1)
                val amount = matcher.group(2)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                val receiver = matcher.group(3)?.trim() ?: "Unknown"
                val balance = matcher.group(6)?.replace(",", "")?.toDoubleOrNull()
                val isFuliza = cleanBody.contains("Fuliza", true)
                
                return ParsedTransaction(
                    transactionCode = code,
                    amount = amount,
                    type = TransactionType.EXPENSE,
                    category = if (isFuliza) "Fuliza Payment" else guessCategory(receiver, "Transfer"),
                    provider = if (isFuliza) "M-Pesa Fuliza" else "M-Pesa",
                    sender = "My Wallet",
                    receiver = receiver,
                    accountName = null,
                    balance = balance,
                    date = defaultDate,
                    description = if (isFuliza) "Sent to $receiver (Fuliza)" else "Sent to $receiver",
                    rawBody = cleanBody
                )
            }

            // 9. Paybill
            matcher = paybillPattern.matcher(cleanBody)
            if (matcher.find()) {
                val code = matcher.group(1)
                val amount = matcher.group(2)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                val merchant = matcher.group(3)?.trim() ?: "Merchant"
                val balance = matcher.group(6)?.replace(",", "")?.toDoubleOrNull()
                
                return ParsedTransaction(
                    transactionCode = code,
                    amount = amount,
                    type = TransactionType.EXPENSE,
                    category = guessCategory(merchant, "Bills"),
                    provider = "M-Pesa",
                    sender = "My Wallet",
                    receiver = merchant,
                    accountName = null,
                    balance = balance,
                    date = defaultDate,
                    description = "Payment to $merchant",
                    rawBody = cleanBody
                )
            }

            // 10. Global Generic Fallback
            matcher = globalTransactionPattern.matcher(cleanBody)
            if (matcher.find()) {
                val amount = matcher.group(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                val counterpart = matcher.group(2)?.trim() ?: "Unknown"
                val isDebit = cleanBody.contains("paid", true) || cleanBody.contains("sent", true) || 
                             cleanBody.contains("spent", true) || cleanBody.contains("debited", true) ||
                             cleanBody.contains("withdrawal", true) || cleanBody.contains("purchase", true) ||
                             cleanBody.contains("bought", true)
                
                return ParsedTransaction(
                    transactionCode = "GLO_" + System.currentTimeMillis().toString().takeLast(6),
                    amount = amount,
                    type = if (isDebit) TransactionType.EXPENSE else TransactionType.INCOME,
                    category = guessCategory(counterpart, if (isDebit) "Expense" else "Income"),
                    provider = smsSender,
                    sender = if (isDebit) "My Wallet" else counterpart,
                    receiver = if (isDebit) counterpart else "My Wallet",
                    accountName = null,
                    balance = null,
                    date = defaultDate,
                    description = if (isDebit) "Payment to $counterpart" else "Received from $counterpart",
                    rawBody = cleanBody
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun guessCategory(text: String, fallback: String): String {
        val name = text.lowercase()
        return when {
            name.contains("netflix") || name.contains("spotify") || name.contains("showmax") || 
            name.contains("youtube") || name.contains("apple") || name.contains("google") ||
            name.contains("disney") || name.contains("amazon") || name.contains("sub") -> "Subscription"
            
            name.contains("kplc") || name.contains("power") || name.contains("light") ||
            name.contains("water") || name.contains("token") -> "Utilities"
            
            name.contains("super") || name.contains("naivas") || name.contains("carre") || 
            name.contains("quick") || name.contains("grocery") || name.contains("mart") -> "Groceries"
            
            name.contains("uber") || name.contains("bolt") || name.contains("taxi") || 
            name.contains("little") || name.contains("cab") || name.contains("fuel") ||
            name.contains("shell") || name.contains("total") || name.contains("rubis") ||
            name.contains("petrol") -> "Transport"
            
            name.contains("rent") || name.contains("landlord") || name.contains("prop") -> "Rent"
            
            name.contains("hosp") || name.contains("pharm") || name.contains("chem") || 
            name.contains("health") || name.contains("clinic") || name.contains("dent") -> "Health"
            
            name.contains("school") || name.contains("college") || name.contains("uni") || 
            name.contains("fees") || name.contains("edu") -> "Education"
            
            name.contains("sacco") || name.contains("chama") || name.contains("shares") || 
            name.contains("saving") || name.contains("invest") || name.contains("m-shwari") ||
            name.contains("kcb m-pesa") -> "Savings"
            
            name.contains("loan") || name.contains("interest") || name.contains("tala") || 
            name.contains("branch") || name.contains("zenka") || name.contains("fuliza") ||
            name.contains("credit") || name.contains("debt") -> "Loan"
            
            name.contains("hotel") || name.contains("rest") || name.contains("cafe") || 
            name.contains("java") || name.contains("kfc") || name.contains("pizza") ||
            name.contains("eat") || name.contains("food") -> "Dining Out"
            
            name.contains("airtime") || name.contains("bundle") || name.contains("data") -> "Airtime"
            
            name.contains("bet") || name.contains("sport") || name.contains("gambl") -> "Gambling"

            else -> fallback
        }
    }
}
