package com.ajyra.amarhishab

import com.ajyra.amarhishab.data.service.ColumnMapping
import com.ajyra.amarhishab.data.service.DataImportService
import com.ajyra.amarhishab.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DataImportTest {

    @Test
    fun testDateParsing() {
        assertEquals("2026-09-24", DataImportService.parseDate("2026-09-24"))
        assertEquals("2026-09-24", DataImportService.parseDate("24-09-2026"))
        assertEquals("2026-09-24", DataImportService.parseDate("24/09/2026"))
        assertEquals("2026-09-24", DataImportService.parseDate("09/24/2026"))
        assertEquals("2026-09-24", DataImportService.parseDate("2026/09/24"))
        assertEquals("2026-09-24", DataImportService.parseDate("24.09.2026"))
        assertEquals("2026-09-24", DataImportService.parseDate("24 Sep 2026"))
        assertEquals("2026-09-24", DataImportService.parseDate("24 September 2026"))
        
        // Bengali numerals
        assertEquals("2026-09-24", DataImportService.parseDate("২০২৬-০৯-২৪"))
        assertEquals("2026-09-24", DataImportService.parseDate("২৪-০৯-২০২৬"))
    }

    @Test
    fun testSummaryRowFiltering() {
        assertTrue(DataImportService.isSummaryOrHeaderRow(listOf("Total Income", "৳1,000.00")))
        assertTrue(DataImportService.isSummaryOrHeaderRow(listOf("মোট আয়", "১,০০০.০০")))
        assertTrue(DataImportService.isSummaryOrHeaderRow(listOf("মোট ব্যয়", "৫০০.০০")))
        assertTrue(DataImportService.isSummaryOrHeaderRow(listOf("Total Expense", "500.00")))
        assertTrue(DataImportService.isSummaryOrHeaderRow(listOf("Net Balance", "500.00")))
        assertTrue(DataImportService.isSummaryOrHeaderRow(listOf("নীট স্থিতি", "৫০০.০০")))
        assertTrue(DataImportService.isSummaryOrHeaderRow(listOf("AMAR HISHAB", "Financial Statement")))
        assertTrue(DataImportService.isSummaryOrHeaderRow(listOf("Period: All Time")))
        assertTrue(DataImportService.isSummaryOrHeaderRow(listOf("Generated: 24 Sep 2026")))
        assertTrue(DataImportService.isSummaryOrHeaderRow(listOf("Date", "Type", "Category", "Account", "Note", "Amount")))

        // Actual transaction rows must NOT be summary rows
        assertFalse(DataImportService.isSummaryOrHeaderRow(listOf("2026-09-24", "Expense", "Food", "Cash", "Lunch", "150.00")))
        assertFalse(DataImportService.isSummaryOrHeaderRow(listOf("2026-09-24", "Income", "Salary", "Bank", "Bonus", "50000.00")))
        assertFalse(DataImportService.isSummaryOrHeaderRow(listOf("2026-09-24", "Transfer", "Transfer", "Cash", "CASH -> BKASH", "500.00")))
    }

    @Test
    fun testCsvLineParsing() {
        val line = "2026-09-24,Expense,\"Food & Dining\",Cash,\"Lunch, with team\",150.00"
        val cols = DataImportService.parseCsvLine(line)
        assertEquals(6, cols.size)
        assertEquals("2026-09-24", cols[0])
        assertEquals("Expense", cols[1])
        assertEquals("Food & Dining", cols[2])
        assertEquals("Cash", cols[3])
        assertEquals("Lunch, with team", cols[4])
        assertEquals("150.00", cols[5])
    }

    @Test
    fun testPdfStreamDecompressionAndParsing() {
        // Construct a synthetic PDF content stream with /FlateDecode compression
        val content = """
            BT
            /F1 10 Tf
            (2026-09-24) Tj
            (Expense) Tj
            (Food) Tj
            (Cash) Tj
            (Lunch) Tj
            (150.00) Tj
            T*
            (2026-09-25) Tj
            (Income) Tj
            (Salary) Tj
            (Bank) Tj
            (-) Tj
            (25000.00) Tj
            ET
        """.trimIndent()

        val deflater = Deflater()
        deflater.setInput(content.toByteArray(StandardCharsets.UTF_8))
        deflater.finish()
        val baos = ByteArrayOutputStream()
        val buf = ByteArray(1024)
        while (!deflater.finished()) {
            val count = deflater.deflate(buf)
            baos.write(buf, 0, count)
        }
        deflater.end()
        val compressed = baos.toByteArray()

        // Build a mock PDF file containing the stream
        val pdfSb = StringBuilder()
        pdfSb.append("%PDF-1.4\n")
        pdfSb.append("1 0 obj\n")
        pdfSb.append("<< /Length ${compressed.size} /Filter /FlateDecode >>\n")
        pdfSb.append("stream\r\n")

        val pdfHeaderBytes = pdfSb.toString().toByteArray(StandardCharsets.US_ASCII)
        val pdfFooterBytes = "\r\nendstream\r\nendobj\r\n%%EOF".toByteArray(StandardCharsets.US_ASCII)

        val fullPdfBytes = pdfHeaderBytes + compressed + pdfFooterBytes

        val parsedRows = DataImportService.parsePdfStream(ByteArrayInputStream(fullPdfBytes))
        assertTrue(parsedRows.isNotEmpty())
        assertEquals("2026-09-24", parsedRows[0][0])
    }
}
