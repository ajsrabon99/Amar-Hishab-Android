package com.ajyra.amarhishab.data.service

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.ajyra.amarhishab.data.local.CategoryManager
import com.ajyra.amarhishab.model.Transaction
import com.ajyra.amarhishab.model.TransactionType
import com.ajyra.amarhishab.utils.CurrencyFormatter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StatementExportService {

    fun generateCsv(
        context: Context,
        transactions: List<Transaction>,
        periodLabel: String,
        isBengali: Boolean
    ): File {
        val fileName = "AmarHishab_Statement_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)

        val categoryManager = CategoryManager.getInstance(context)

        FileOutputStream(file).use { fos ->
            // Write UTF-8 BOM so spreadsheet apps (Excel, Google Sheets) parse Bengali and Unicode cleanly
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                // Header comments
                writer.append("# Amar Hishab Financial Statement\n")
                writer.append("# Period: ").append(periodLabel).append("\n")
                writer.append("# Generated on: ").append(SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())).append("\n\n")

                // CSV Headers
                val headers = if (isBengali) {
                    listOf("তারিখ", "ধরন", "ক্যাটাগরি", "অ্যাকাউন্ট", "পরিমাণ (টাকা)", "বিবরণ")
                } else {
                    listOf("Date", "Type", "Category", "Account", "Amount (BDT)", "Note/Description")
                }
                writer.append(headers.joinToString(",") { escapeCsv(it) }).append("\n")

                // Rows
                for (tx in transactions) {
                    val typeLabel = when (tx.type) {
                        TransactionType.INCOME -> if (isBengali) "আয়" else "Income"
                        TransactionType.EXPENSE -> if (isBengali) "ব্যয়" else "Expense"
                        TransactionType.TRANSFER -> if (isBengali) "স্থানান্তর" else "Transfer"
                    }
                    val catName = categoryManager.getCategoryDisplayName(tx.category, isBengali)
                    val note = tx.description ?: ""

                    val row = listOf(
                        tx.date,
                        typeLabel,
                        catName,
                        tx.accountCode,
                        String.format(Locale.US, "%.2f", tx.amount),
                        note
                    )
                    writer.append(row.joinToString(",") { escapeCsv(it) }).append("\n")
                }

                // Summary Row
                val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                val net = totalIncome - totalExpense

                writer.append("\n")
                writer.append(if (isBengali) "# মোট আয়," else "# Total Income,").append(String.format(Locale.US, "%.2f", totalIncome)).append("\n")
                writer.append(if (isBengali) "# মোট ব্যয়," else "# Total Expense,").append(String.format(Locale.US, "%.2f", totalExpense)).append("\n")
                writer.append(if (isBengali) "# নীট ব্যালেন্স," else "# Net Balance,").append(String.format(Locale.US, "%.2f", net)).append("\n")
            }
        }
        return file
    }

    private fun escapeCsv(value: String): String {
        var str = value.replace("\"", "\"\"")
        if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            str = "\"$str\""
        }
        return str
    }

    fun generatePdf(
        context: Context,
        transactions: List<Transaction>,
        periodLabel: String,
        totalIncome: Double,
        totalExpense: Double,
        netBalance: Double,
        isBengali: Boolean
    ): File {
        val categoryManager = CategoryManager.getInstance(context)
        val document = PdfDocument()

        val pageWidth = 595 // Standard A4 width in points
        val pageHeight = 842 // Standard A4 height in points
        val margin = 36f

        val titlePaint = Paint().apply {
            color = Color.rgb(32, 42, 69) // #202A45
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subPaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 10f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val cardBgPaint = Paint().apply {
            color = Color.rgb(246, 247, 250) // #F6F7FA
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 9f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val boldTextPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = Color.rgb(32, 42, 69)
            isAntiAlias = true
        }

        val headerTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 0.8f
            isAntiAlias = true
        }

        val greenPaint = Paint().apply {
            color = Color.rgb(0, 137, 123)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val redPaint = Paint().apply {
            color = Color.rgb(229, 57, 53)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        // Layout constants
        val rowHeight = 22f
        val colX = floatArrayOf(
            margin,             // Date
            margin + 65f,       // Type
            margin + 120f,      // Category
            margin + 225f,      // Account
            margin + 295f,      // Note
            pageWidth - margin - 85f // Amount
        )

        val rowsPerPageContinuation = 30
        val rowsPageOne = 18

        var pageIndex = 0
        var currentTxIndex = 0
        val totalTransactions = transactions.size

        while (currentTxIndex < totalTransactions || pageIndex == 0) {
            pageIndex++
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            var y = margin

            if (pageIndex == 1) {
                // Brand Header
                canvas.drawText("AMAR HISHAB", margin, y + 16f, titlePaint)
                canvas.drawText(
                    if (isBengali) "ব্যক্তিগত আর্থিক হিসাব বিবরণী" else "Personal Financial Statement",
                    margin,
                    y + 30f,
                    subPaint
                )

                // Date Generated
                val genDate = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(Date())
                val genText = (if (isBengali) "তৈরি: " else "Generated: ") + genDate
                val genWidth = subPaint.measureText(genText)
                canvas.drawText(genText, pageWidth - margin - genWidth, y + 16f, subPaint)

                // Statement Period Badge
                val periodText = (if (isBengali) "সময়সীমা: " else "Period: ") + periodLabel
                val periodWidth = subPaint.measureText(periodText)
                canvas.drawText(periodText, pageWidth - margin - periodWidth, y + 30f, subPaint)

                y += 45f

                // Summary Cards
                val cardWidth = (pageWidth - 2 * margin - 16f) / 3f
                val cardHeight = 52f

                // Card 1: Income
                canvas.drawRoundRect(RectF(margin, y, margin + cardWidth, y + cardHeight), 8f, 8f, cardBgPaint)
                canvas.drawText(if (isBengali) "মোট আয়" else "Total Income", margin + 10f, y + 18f, subPaint)
                canvas.drawText(CurrencyFormatter.format(totalIncome, false), margin + 10f, y + 38f, greenPaint)

                // Card 2: Expense
                val c2X = margin + cardWidth + 8f
                canvas.drawRoundRect(RectF(c2X, y, c2X + cardWidth, y + cardHeight), 8f, 8f, cardBgPaint)
                canvas.drawText(if (isBengali) "মোট ব্যয়" else "Total Expense", c2X + 10f, y + 18f, subPaint)
                canvas.drawText(CurrencyFormatter.format(totalExpense, false), c2X + 10f, y + 38f, redPaint)

                // Card 3: Net Balance
                val c3X = c2X + cardWidth + 8f
                canvas.drawRoundRect(RectF(c3X, y, c3X + cardWidth, y + cardHeight), 8f, 8f, cardBgPaint)
                canvas.drawText(if (isBengali) "নীট স্থিতি" else "Net Balance", c3X + 10f, y + 18f, subPaint)
                val netColor = if (netBalance >= 0) greenPaint else redPaint
                canvas.drawText(CurrencyFormatter.format(netBalance, false), c3X + 10f, y + 38f, netColor)

                y += cardHeight + 20f
            } else {
                // Continuation header
                canvas.drawText("Amar Hishab - Financial Statement (Continued)", margin, y + 12f, subPaint)
                canvas.drawText("Period: $periodLabel", pageWidth - margin - 150f, y + 12f, subPaint)
                y += 24f
            }

            // Table Header Bar
            canvas.drawRoundRect(RectF(margin, y, pageWidth - margin, y + 22f), 4f, 4f, headerPaint)
            val headerY = y + 14f
            canvas.drawText(if (isBengali) "তারিখ" else "Date", colX[0] + 4f, headerY, headerTextPaint)
            canvas.drawText(if (isBengali) "ধরন" else "Type", colX[1], headerY, headerTextPaint)
            canvas.drawText(if (isBengali) "খাত" else "Category", colX[2], headerY, headerTextPaint)
            canvas.drawText(if (isBengali) "অ্যাকাউন্ট" else "Account", colX[3], headerY, headerTextPaint)
            canvas.drawText(if (isBengali) "বিবরণ" else "Note", colX[4], headerY, headerTextPaint)
            canvas.drawText(if (isBengali) "টাকা" else "Amount", colX[5], headerY, headerTextPaint)

            y += 26f

            // Rows on this page
            val maxRows = if (pageIndex == 1) rowsPageOne else rowsPerPageContinuation
            var rowsOnThisPage = 0

            if (totalTransactions == 0) {
                canvas.drawText(
                    if (isBengali) "নির্বাচিত সময়ে কোনো লেনদেন পাওয়া যায়নি।" else "No transactions found for the selected period.",
                    margin + 10f,
                    y + 18f,
                    subPaint
                )
                y += 28f
            } else {
                while (currentTxIndex < totalTransactions && rowsOnThisPage < maxRows) {
                    val tx = transactions[currentTxIndex]
                    val rowY = y + 14f

                    // Alternating subtle background
                    if (rowsOnThisPage % 2 == 1) {
                        canvas.drawRect(RectF(margin, y, pageWidth - margin, y + rowHeight), cardBgPaint)
                    }

                    // Date
                    canvas.drawText(tx.date, colX[0] + 4f, rowY, textPaint)

                    // Type
                    val typeStr = when (tx.type) {
                        TransactionType.INCOME -> if (isBengali) "আয়" else "Income"
                        TransactionType.EXPENSE -> if (isBengali) "ব্যয়" else "Expense"
                        TransactionType.TRANSFER -> if (isBengali) "ট্রান্সফার" else "Transfer"
                    }
                    val typeP = if (tx.type == TransactionType.INCOME) greenPaint else if (tx.type == TransactionType.EXPENSE) redPaint else textPaint
                    canvas.drawText(typeStr, colX[1], rowY, typeP)

                    // Category
                    val catName = categoryManager.getCategoryDisplayName(tx.category, isBengali)
                    val clippedCat = if (catName.length > 18) catName.substring(0, 16) + ".." else catName
                    canvas.drawText(clippedCat, colX[2], rowY, boldTextPaint)

                    // Account
                    canvas.drawText(tx.accountCode, colX[3], rowY, textPaint)

                    // Note
                    val noteStr = tx.description ?: "-"
                    val clippedNote = if (noteStr.length > 22) noteStr.substring(0, 20) + ".." else noteStr
                    canvas.drawText(clippedNote, colX[4], rowY, subPaint)

                    // Amount
                    val amountStr = String.format(Locale.US, "%.2f", tx.amount)
                    canvas.drawText(amountStr, colX[5], rowY, if (tx.type == TransactionType.INCOME) greenPaint else redPaint)

                    // Divider line
                    canvas.drawLine(margin, y + rowHeight, pageWidth - margin, y + rowHeight, linePaint)

                    y += rowHeight
                    currentTxIndex++
                    rowsOnThisPage++
                }
            }

            // Footer
            val footerY = pageHeight - margin + 10f
            canvas.drawLine(margin, footerY - 14f, pageWidth - margin, footerY - 14f, linePaint)
            canvas.drawText("Amar Hishab • amarhishab.app", margin, footerY, subPaint)

            val pageLabel = "Page $pageIndex"
            canvas.drawText(pageLabel, pageWidth - margin - subPaint.measureText(pageLabel), footerY, subPaint)

            document.finishPage(page)

            if (currentTxIndex >= totalTransactions) break
        }

        val fileName = "AmarHishab_Statement_${System.currentTimeMillis()}.pdf"
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
        return file
    }

    fun shareFile(context: Context, file: File, mimeType: String, title: String = "Share Statement") {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
