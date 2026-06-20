package com.musafinance.pesamate.utils

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.musafinance.pesamate.data.local.TransactionEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExportHelper {
    
    /**
     * Export transactions to PDF
     */
    fun exportToPDF(
        context: Context,
        transactions: List<TransactionEntity>,
        startDate: Long,
        endDate: Long
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas
            val paint = Paint()
            
            // Title
            paint.textSize = 20f
            paint.isFakeBoldText = true
            canvas.drawText("PesaMate Financial Report", 50f, 50f, paint)
            
            // Date range
            paint.textSize = 12f
            paint.isFakeBoldText = false
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            canvas.drawText(
                "Period: ${sdf.format(Date(startDate))} - ${sdf.format(Date(endDate))}",
                50f, 80f, paint
            )
            
            // Summary
            val income = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
            val expenses = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val net = income - expenses
            
            var yPos = 120f
            paint.textSize = 14f
            paint.isFakeBoldText = true
            canvas.drawText("Summary", 50f, yPos, paint)
            
            paint.isFakeBoldText = false
            paint.textSize = 12f
            yPos += 25f
            canvas.drawText("Total Income: KSh ${String.format("%,.2f", income)}", 50f, yPos, paint)
            yPos += 20f
            canvas.drawText("Total Expenses: KSh ${String.format("%,.2f", expenses)}", 50f, yPos, paint)
            yPos += 20f
            canvas.drawText("Net: KSh ${String.format("%,.2f", net)}", 50f, yPos, paint)
            
            // Transactions
            yPos += 40f
            paint.textSize = 14f
            paint.isFakeBoldText = true
            canvas.drawText("Transactions (${transactions.size})", 50f, yPos, paint)
            
            paint.textSize = 10f
            paint.isFakeBoldText = false
            yPos += 30f
            
            // Table headers
            canvas.drawText("Date", 50f, yPos, paint)
            canvas.drawText("Description", 150f, yPos, paint)
            canvas.drawText("Category", 350f, yPos, paint)
            canvas.drawText("Amount", 480f, yPos, paint)
            yPos += 5f
            canvas.drawLine(50f, yPos, 545f, yPos, paint)
            yPos += 15f
            
            var pageNum = 1
            val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
            
            for (tx in transactions.take(50)) { // Limit to 50 transactions per page for simplicity
                if (yPos > 800f) {
                    // Finish current page and start new one
                    pdfDocument.finishPage(page)
                    pageNum++
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    yPos = 50f
                }
                
                canvas.drawText(dateFormat.format(Date(tx.date)), 50f, yPos, paint)
                canvas.drawText(tx.description.take(25), 150f, yPos, paint)
                canvas.drawText(tx.category, 350f, yPos, paint)
                val amountColor = if (tx.type == "INCOME") android.graphics.Color.GREEN else android.graphics.Color.RED
                paint.color = amountColor
                canvas.drawText("${if (tx.type == "INCOME") "+" else "-"}${String.format("%,.0f", tx.amount)}", 480f, yPos, paint)
                paint.color = android.graphics.Color.BLACK
                yPos += 20f
            }
            
            pdfDocument.finishPage(page)
            
            // Save to file
            val fileName = "PesaMate_Report_${System.currentTimeMillis()}.pdf"
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "PesaMate")
            if (!dir.exists()) dir.mkdirs()
            
            val file = File(dir, fileName)
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            
            Log.d("ExportHelper", "PDF saved to: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e("ExportHelper", "Error creating PDF", e)
            null
        }
    }
    
    /**
     * Export transactions to CSV (Excel-compatible)
     */
    fun exportToCSV(
        context: Context,
        transactions: List<TransactionEntity>,
        startDate: Long,
        endDate: Long
    ): File? {
        return try {
            val fileName = "PesaMate_Export_${System.currentTimeMillis()}.csv"
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "PesaMate")
            if (!dir.exists()) dir.mkdirs()
            
            val file = File(dir, fileName)
            val writer = FileOutputStream(file).bufferedWriter()
            
            // Write header
            writer.write("Date,Type,Category,Description,Amount,Provider,Balance\n")
            
            // Write data
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            for (tx in transactions) {
                writer.write("${dateFormat.format(Date(tx.date))},")
                writer.write("${tx.type},")
                writer.write("${tx.category},")
                writer.write("\"${tx.description.replace("\"", "\"\"")}\",")
                writer.write("${tx.amount},")
                writer.write("${tx.provider},")
                writer.write("${tx.balance ?: 0.0}\n")
            }
            
            writer.close()
            Log.d("ExportHelper", "CSV saved to: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e("ExportHelper", "Error creating CSV", e)
            null
        }
    }
    
    /**
     * Share file via Android share sheet
     */
    fun shareFile(context: Context, file: File, mimeType: String = "application/pdf") {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(intent, "Share Report"))
        } catch (e: Exception) {
            Log.e("ExportHelper", "Error sharing file", e)
        }
    }
}
