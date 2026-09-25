package com.ajyra.amarhishab.data.service

import android.content.Context
import android.net.Uri
import com.ajyra.amarhishab.data.local.CategoryManager
import com.ajyra.amarhishab.data.local.TransactionDao
import com.ajyra.amarhishab.data.local.TransactionEntity
import com.ajyra.amarhishab.model.AccountType
import com.ajyra.amarhishab.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.Inflater
import java.util.zip.ZipInputStream
import kotlin.math.max

data class ColumnMapping(
    val dateColIndex: Int = -1,
    val amountColIndex: Int = -1,
    val typeColIndex: Int = -1,
    val categoryColIndex: Int = -1,
    val accountColIndex: Int = -1,
    val noteColIndex: Int = -1
) {
    val isValid: Boolean get() = dateColIndex >= 0 && amountColIndex >= 0
}

data class ParsedImportRow(
    val rowNumber: Int,
    val date: String,
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val accountCode: String,
    val toAccountCode: String? = null,
    val note: String?,
    val isValid: Boolean,
    val isDuplicate: Boolean = false,
    val errorReason: String? = null
)

data class ImportAnalysis(
    val fileName: String,
    val rawHeaders: List<String>,
    val detectedMapping: ColumnMapping,
    val totalRowsCount: Int,
    val validRows: List<ParsedImportRow>,
    val invalidRows: List<ParsedImportRow>,
    val duplicateRows: List<ParsedImportRow>,
    val newCategories: List<String>,
    val sampleRows: List<ParsedImportRow>,
    val detectedFilesInsideZip: List<String> = emptyList()
)

data class ImportResult(
    val totalRows: Int,
    val importedCount: Int,
    val skippedDuplicates: Int,
    val skippedErrors: Int,
    val newCategoriesCreated: Int
)

object DataImportService {

    private val supportedDateFormats = listOf(
        "yyyy-MM-dd",
        "dd/MM/yyyy",
        "dd-MM-yyyy",
        "yyyy/MM/dd",
        "MM/dd/yyyy",
        "d/M/yyyy",
        "dd.MM.yyyy",
        "yyyy.MM.dd",
        "dd MMM yyyy",
        "d MMM yyyy",
        "dd-MMM-yyyy",
        "d-MMM-yyyy",
        "yyyy-MM-dd HH:mm",
        "dd/MM/yyyy HH:mm"
    )

    private val targetDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun normalizeBengaliDigits(input: String): String {
        val bengali = "০১২৩৪৫৬৭৮৯"
        val english = "0123456789"
        var res = input
        for (i in 0..9) {
            res = res.replace(bengali[i], english[i])
        }
        return res
    }

    fun parseDate(raw: String): String? {
        if (raw.isBlank()) return null
        val clean = normalizeBengaliDigits(raw.trim())
            .replace(",", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        // 1. Try standard text date formats
        for (pattern in supportedDateFormats) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
                val parsed = parser.parse(clean)
                if (parsed != null) {
                    return targetDateFormat.format(parsed)
                }
            } catch (_: Exception) {
            }
        }

        // 2. Try month name variations
        val monthPatterns = listOf("MMM dd yyyy", "MMMM dd yyyy", "dd MMMM yyyy", "d MMMM yyyy")
        for (pattern in monthPatterns) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.ENGLISH).apply { isLenient = true }
                val parsed = parser.parse(clean)
                if (parsed != null) {
                    return targetDateFormat.format(parsed)
                }
            } catch (_: Exception) {
            }
        }

        // 3. Try Excel serial date number
        val num = clean.toDoubleOrNull()
        if (num != null && num in 30000.0..70000.0) {
            try {
                val epochMillis = ((num - 25569.0) * 86400.0 * 1000.0).toLong()
                return targetDateFormat.format(Date(epochMillis))
            } catch (_: Exception) {
            }
        }

        return null
    }

    fun isSummaryOrHeaderRow(cols: List<String>): Boolean {
        if (cols.isEmpty()) return true
        val joined = cols.joinToString(" ").lowercase().trim()
        val firstCol = cols.first().lowercase().trim()

        // Brand & report titles
        if (joined.contains("amar hisab") || joined.contains("statement") || joined.contains("বিবরণী") ||
            joined.contains("period:") || joined.contains("সময়সীমা") || joined.contains("generated:") || joined.contains("তৈরি:")) {
            return true
        }

        // Column header indicators
        if (firstCol.contains("date") || firstCol.contains("তারিখ") || firstCol == "id" || firstCol == "#") {
            return true
        }

        // Summary values: MUST NEVER BECOME TRANSACTIONS
        if (joined.contains("total income") || joined.contains("মোট আয়") || joined.contains("মোট আয়") ||
            joined.contains("total expense") || joined.contains("মোট ব্যয়") || joined.contains("মোট ব্যয়") ||
            joined.contains("net balance") || joined.contains("নীট স্থিতি") || joined.contains("নীট ব্যালেন্স") ||
            joined.contains("closing balance") || joined.contains("opening balance") || joined.contains("starting balance")) {
            return true
        }

        return false
    }

    suspend fun analyzeFile(
        context: Context,
        uri: Uri,
        fileName: String,
        dao: TransactionDao,
        customMapping: ColumnMapping? = null
    ): ImportAnalysis = withContext(Dispatchers.IO) {
        val lowerName = fileName.lowercase()
        val isZip = lowerName.endsWith(".zip")
        val isXlsx = lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls")
        val isPdf = lowerName.endsWith(".pdf")

        val csvLines = mutableListOf<List<String>>()
        val zipFilesFound = mutableListOf<String>()

        context.contentResolver.openInputStream(uri)?.use { stream ->
            when {
                isZip -> {
                    val zis = ZipInputStream(stream)
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val entryName = entry.name
                        if (!entry.isDirectory) {
                            val entryLower = entryName.lowercase()
                            if (entryLower.endsWith(".csv") || entryLower.endsWith(".txt")) {
                                zipFilesFound.add(entryName)
                                val reader = BufferedReader(InputStreamReader(zis, StandardCharsets.UTF_8))
                                var isFirstLine = csvLines.isEmpty()
                                reader.forEachLine { line ->
                                    val cols = parseCsvLine(line)
                                    if (cols.isNotEmpty() && cols.any { it.isNotBlank() }) {
                                        if (isFirstLine) {
                                            csvLines.add(cols)
                                            isFirstLine = false
                                        } else if (!isSummaryOrHeaderRow(cols)) {
                                            csvLines.add(cols)
                                        }
                                    }
                                }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
                isXlsx -> {
                    val rows = parseXlsxStream(stream)
                    csvLines.addAll(rows)
                }
                isPdf -> {
                    val rows = parsePdfStream(stream)
                    csvLines.addAll(rows)
                }
                else -> {
                    val reader = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8))
                    reader.forEachLine { line ->
                        val cols = parseCsvLine(line)
                        if (cols.isNotEmpty() && cols.any { it.isNotBlank() }) {
                            csvLines.add(cols)
                        }
                    }
                }
            }
        }

        if (csvLines.isEmpty()) {
            return@withContext ImportAnalysis(
                fileName = fileName,
                rawHeaders = emptyList(),
                detectedMapping = ColumnMapping(),
                totalRowsCount = 0,
                validRows = emptyList(),
                invalidRows = emptyList(),
                duplicateRows = emptyList(),
                newCategories = emptyList(),
                sampleRows = emptyList(),
                detectedFilesInsideZip = zipFilesFound
            )
        }

        // Find true table header row if present, otherwise default
        val headerIndex = csvLines.indexOfFirst { cols ->
            val first = cols.firstOrNull()?.lowercase() ?: ""
            val joined = cols.joinToString(" ").lowercase()
            (first.contains("date") || first.contains("তারিখ") || first == "id" || first == "#") &&
            (joined.contains("amount") || joined.contains("টাকা") || joined.contains("category") || joined.contains("type"))
        }

        val rawHeaders = if (headerIndex >= 0) {
            csvLines[headerIndex]
        } else {
            listOf("Date", "Type", "Category", "Account", "Note", "Amount")
        }

        val dataRows = if (headerIndex >= 0) {
            csvLines.subList(headerIndex + 1, csvLines.size).filter { !isSummaryOrHeaderRow(it) }
        } else {
            csvLines.filter { !isSummaryOrHeaderRow(it) }
        }

        val mapping = customMapping ?: autoDetectColumns(rawHeaders)

        // Load existing transactions for duplicate detection
        val existingEntities = dao.getAllTransactionsList()
        val existingKeys = existingEntities.map {
            "${it.date.trim()}_${String.format(Locale.US, "%.2f", it.amount)}_${it.type.trim().uppercase()}_${it.category.trim().lowercase()}"
        }.toSet()

        val categoryManager = CategoryManager.getInstance(context)
        val defaultAndCustomCategories = categoryManager.getAllCategories().map { it.nameEn.lowercase() to it.nameBn.lowercase() }
        val knownCategoryNames = mutableSetOf<String>().apply {
            defaultAndCustomCategories.forEach { (en, bn) ->
                add(en)
                add(bn)
            }
        }

        val validList = mutableListOf<ParsedImportRow>()
        val invalidList = mutableListOf<ParsedImportRow>()
        val duplicateList = mutableListOf<ParsedImportRow>()
        val detectedNewCategories = mutableSetOf<String>()

        dataRows.forEachIndexed { index, cols ->
            if (isSummaryOrHeaderRow(cols)) return@forEachIndexed
            val rowNum = index + 1
            val parseResult = parseRow(cols, mapping, rowNum)
            if (parseResult.isValid) {
                val key = "${parseResult.date}_${String.format(Locale.US, "%.2f", parseResult.amount)}_${parseResult.type.value}_${parseResult.category.trim().lowercase()}"
                if (existingKeys.contains(key)) {
                    val dup = parseResult.copy(isDuplicate = true)
                    duplicateList.add(dup)
                } else {
                    validList.add(parseResult)
                    val catTrimmed = parseResult.category.trim()
                    if (catTrimmed.isNotEmpty() && !knownCategoryNames.contains(catTrimmed.lowercase())) {
                        detectedNewCategories.add(catTrimmed)
                    }
                }
            } else {
                // If it's not a summary row, record as invalid
                if (parseResult.errorReason?.contains("Summary") != true) {
                    invalidList.add(parseResult)
                }
            }
        }

        val sampleRows = (validList + duplicateList + invalidList).take(6)

        ImportAnalysis(
            fileName = fileName,
            rawHeaders = rawHeaders,
            detectedMapping = mapping,
            totalRowsCount = dataRows.size,
            validRows = validList,
            invalidRows = invalidList,
            duplicateRows = duplicateList,
            newCategories = detectedNewCategories.toList(),
            sampleRows = sampleRows,
            detectedFilesInsideZip = zipFilesFound
        )
    }

    private fun autoDetectColumns(headers: List<String>): ColumnMapping {
        var dateIdx = -1
        var amountIdx = -1
        var typeIdx = -1
        var categoryIdx = -1
        var accountIdx = -1
        var noteIdx = -1

        headers.forEachIndexed { index, headerRaw ->
            val h = headerRaw.trim().lowercase()
            when {
                dateIdx == -1 && (h.contains("date") || h.contains("তারিখ") || h.contains("time") || h.contains("সময়")) -> dateIdx = index
                amountIdx == -1 && (h.contains("amount") || h.contains("টাকা") || h.contains("পরিমাণ") || h.contains("bdt") || h.contains("taka") || h.contains("মূল্য") || h.contains("debit") || h.contains("credit")) -> amountIdx = index
                typeIdx == -1 && (h.contains("type") || h.contains("ধরন") || h.contains("প্রকার") || h.contains("txn") || h.contains("status")) -> typeIdx = index
                categoryIdx == -1 && (h.contains("category") || h.contains("cat") || h.contains("ক্যাটাগরি") || h.contains("খাত") || h.contains("বিভাগ")) -> categoryIdx = index
                accountIdx == -1 && (h.contains("account") || h.contains("wallet") || h.contains("অ্যাকাউন্ট") || h.contains("ওয়ালেট") || h.contains("source")) -> accountIdx = index
                noteIdx == -1 && (h.contains("note") || h.contains("desc") || h.contains("details") || h.contains("বিবরণ") || h.contains("মন্তব্য")) -> noteIdx = index
            }
        }

        // Fallback heuristics if not found by name
        if (dateIdx == -1 && headers.isNotEmpty()) dateIdx = 0
        if (typeIdx == -1 && headers.size > 1) typeIdx = 1
        if (categoryIdx == -1 && headers.size > 2) categoryIdx = 2
        if (accountIdx == -1 && headers.size > 3) accountIdx = 3
        if (noteIdx == -1 && headers.size > 4) noteIdx = 4
        if (amountIdx == -1 && headers.size > 5) amountIdx = 5
        else if (amountIdx == -1 && headers.size > 1) amountIdx = headers.size - 1

        return ColumnMapping(
            dateColIndex = dateIdx,
            amountColIndex = amountIdx,
            typeColIndex = typeIdx,
            categoryColIndex = categoryIdx,
            accountColIndex = accountIdx,
            noteColIndex = noteIdx
        )
    }

    private fun parseRow(cols: List<String>, mapping: ColumnMapping, rowNumber: Int): ParsedImportRow {
        // Quick summary check
        val allText = cols.joinToString(" ").lowercase()
        if (allText.contains("total income") || allText.contains("মোট আয়") || allText.contains("মোট আয়") ||
            allText.contains("total expense") || allText.contains("মোট ব্যয়") || allText.contains("মোট ব্যয়") ||
            allText.contains("net balance") || allText.contains("নীট স্থিতি") || allText.contains("নীট ব্যালেন্স")) {
            return ParsedImportRow(
                rowNumber = rowNumber,
                date = "",
                amount = 0.0,
                type = TransactionType.EXPENSE,
                category = "",
                accountCode = "",
                note = null,
                isValid = false,
                errorReason = "Summary row (ignored)"
            )
        }

        // Date
        val dateRaw = cols.getOrNull(mapping.dateColIndex)?.trim() ?: ""
        val normalizedDate = parseDate(dateRaw)
        if (normalizedDate == null) {
            return ParsedImportRow(
                rowNumber = rowNumber,
                date = dateRaw,
                amount = 0.0,
                type = TransactionType.EXPENSE,
                category = "",
                accountCode = "",
                note = null,
                isValid = false,
                errorReason = "Invalid date format ($dateRaw)"
            )
        }

        // Amount
        val amountRaw = cols.getOrNull(mapping.amountColIndex)?.trim() ?: ""
        val cleanAmountStr = normalizeBengaliDigits(amountRaw).replace("[^0-9.-]".toRegex(), "")
        val amountVal = cleanAmountStr.toDoubleOrNull()
        if (amountVal == null || amountVal <= 0.0) {
            return ParsedImportRow(
                rowNumber = rowNumber,
                date = normalizedDate,
                amount = 0.0,
                type = TransactionType.EXPENSE,
                category = "",
                accountCode = "",
                note = null,
                isValid = false,
                errorReason = "Invalid amount ($amountRaw)"
            )
        }

        // Type
        val typeRaw = if (mapping.typeColIndex >= 0) cols.getOrNull(mapping.typeColIndex)?.trim()?.lowercase() ?: "" else ""
        val type = when {
            typeRaw.contains("inc") || typeRaw.contains("আয়") || typeRaw.contains("আয়") || typeRaw.contains("credit") || typeRaw.contains("deposit") -> TransactionType.INCOME
            typeRaw.contains("trans") || typeRaw.contains("স্থানান্তর") || typeRaw.contains("ট্রান্সফার") -> TransactionType.TRANSFER
            else -> TransactionType.EXPENSE
        }

        // Category
        val catRaw = if (mapping.categoryColIndex >= 0) cols.getOrNull(mapping.categoryColIndex)?.trim() ?: "" else ""
        val category = if (catRaw.isNotBlank()) catRaw else (if (type == TransactionType.INCOME) "Other Income" else if (type == TransactionType.TRANSFER) "Transfer" else "Other Expense")

        // Account
        val accRaw = if (mapping.accountColIndex >= 0) cols.getOrNull(mapping.accountColIndex)?.trim()?.uppercase() ?: "" else ""
        val accountCode = when {
            accRaw.contains("BKASH") || accRaw.contains("বিকাশ") -> AccountType.BKASH.code
            accRaw.contains("NAGAD") || accRaw.contains("নগদ") -> AccountType.NAGAD.code
            accRaw.contains("BANK") || accRaw.contains("ব্যাংক") -> AccountType.BANK.code
            else -> AccountType.CASH.code
        }

        // Note
        val noteRaw = if (mapping.noteColIndex >= 0) cols.getOrNull(mapping.noteColIndex)?.trim() else null

        // Transfer Destination Account Detection
        var toAccountCode: String? = null
        if (type == TransactionType.TRANSFER) {
            val contextText = (accRaw + " " + (noteRaw ?: "") + " " + catRaw).uppercase()
            toAccountCode = when {
                (contextText.contains("TO BKASH") || contextText.contains("-> BKASH") || contextText.contains("বিকাশ")) && accountCode != AccountType.BKASH.code -> AccountType.BKASH.code
                (contextText.contains("TO NAGAD") || contextText.contains("-> NAGAD") || contextText.contains("নগদ")) && accountCode != AccountType.NAGAD.code -> AccountType.NAGAD.code
                (contextText.contains("TO BANK") || contextText.contains("-> BANK") || contextText.contains("ব্যাংক")) && accountCode != AccountType.BANK.code -> AccountType.BANK.code
                (contextText.contains("TO CASH") || contextText.contains("-> CASH") || contextText.contains("ক্যাশ")) && accountCode != AccountType.CASH.code -> AccountType.CASH.code
                accountCode == AccountType.CASH.code -> AccountType.BKASH.code
                accountCode == AccountType.BKASH.code -> AccountType.CASH.code
                else -> AccountType.CASH.code
            }
        }

        return ParsedImportRow(
            rowNumber = rowNumber,
            date = normalizedDate,
            amount = amountVal,
            type = type,
            category = category,
            accountCode = accountCode,
            toAccountCode = toAccountCode,
            note = noteRaw?.ifBlank { null },
            isValid = true
        )
    }

    fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0

        val cleanLine = if (line.startsWith("\uFEFF")) line.substring(1) else line

        while (i < cleanLine.length) {
            val c = cleanLine[i]
            when {
                c == '\"' -> {
                    if (inQuotes && i + 1 < cleanLine.length && cleanLine[i + 1] == '\"') {
                        sb.append('\"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    result.add(sb.toString().trim())
                    sb.setLength(0)
                }
                else -> {
                    sb.append(c)
                }
            }
            i++
        }
        result.add(sb.toString().trim())
        return result
    }

    fun parseXlsxStream(inputStream: InputStream): List<List<String>> {
        val sharedStrings = mutableListOf<String>()
        val sheetBytesList = mutableListOf<ByteArray>()

        try {
            val zis = ZipInputStream(inputStream)
            var entry = zis.nextEntry
            while (entry != null) {
                val name = entry.name.lowercase()
                if (name == "xl/sharedstrings.xml") {
                    val baos = ByteArrayOutputStream()
                    zis.copyTo(baos)
                    sharedStrings.addAll(parseSharedStrings(baos.toByteArray()))
                } else if (name == "xl/worksheets/sheet1.xml" || (sheetBytesList.isEmpty() && name.startsWith("xl/worksheets/sheet") && name.endsWith(".xml"))) {
                    val baos = ByteArrayOutputStream()
                    zis.copyTo(baos)
                    sheetBytesList.add(baos.toByteArray())
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }

            if (sheetBytesList.isNotEmpty()) {
                return parseWorksheetXml(sheetBytesList.first(), sharedStrings)
            }
        } catch (_: Exception) {
        }
        return emptyList()
    }

    private fun parseSharedStrings(xmlBytes: ByteArray): List<String> {
        val strings = mutableListOf<String>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(xmlBytes.inputStream(), "UTF-8")
            var eventType = parser.eventType
            var inTextTag = false
            val currentSb = StringBuilder()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (parser.name.equals("t", ignoreCase = true)) {
                            inTextTag = true
                        } else if (parser.name.equals("si", ignoreCase = true)) {
                            currentSb.setLength(0)
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inTextTag) {
                            currentSb.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name.equals("t", ignoreCase = true)) {
                            inTextTag = false
                        } else if (parser.name.equals("si", ignoreCase = true)) {
                            strings.add(currentSb.toString())
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {
        }
        return strings
    }

    private fun parseWorksheetXml(xmlBytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(xmlBytes.inputStream(), "UTF-8")
            var eventType = parser.eventType

            var currentRow = mutableListOf<String>()
            var currentCellType = ""
            var currentCellValue = StringBuilder()
            var inValueTag = false
            var inInlineTextTag = false
            var currentCellColIndex = -1

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name.lowercase()) {
                            "row" -> {
                                currentRow = mutableListOf()
                            }
                            "c" -> {
                                currentCellType = parser.getAttributeValue(null, "t") ?: ""
                                val cellRef = parser.getAttributeValue(null, "r") ?: ""
                                currentCellColIndex = extractColumnIndex(cellRef)
                                currentCellValue.setLength(0)
                            }
                            "v" -> {
                                inValueTag = true
                            }
                            "t" -> {
                                if (currentCellType == "inlineStr") {
                                    inInlineTextTag = true
                                }
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inValueTag || inInlineTextTag) {
                            currentCellValue.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (parser.name.lowercase()) {
                            "v" -> inValueTag = false
                            "t" -> inInlineTextTag = false
                            "c" -> {
                                val rawVal = currentCellValue.toString().trim()
                                val resolved = if (currentCellType == "s") {
                                    val idx = rawVal.toIntOrNull()
                                    if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else rawVal
                                } else {
                                    rawVal
                                }
                                if (currentCellColIndex >= 0) {
                                    while (currentRow.size < currentCellColIndex) {
                                        currentRow.add("")
                                    }
                                }
                                currentRow.add(resolved)
                            }
                            "row" -> {
                                if (currentRow.isNotEmpty() && currentRow.any { it.isNotBlank() }) {
                                    rows.add(currentRow)
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {
        }
        return rows
    }

    private fun extractColumnIndex(cellRef: String): Int {
        var col = 0
        var foundChar = false
        for (c in cellRef.uppercase()) {
            if (c in 'A'..'Z') {
                col = col * 26 + (c - 'A' + 1)
                foundChar = true
            } else if (foundChar) {
                break
            }
        }
        return if (col > 0) col - 1 else -1
    }

    /**
     * Parses PDF statement files by extracting decompressed FlateDecode streams,
     * decoding text commands, filtering summary figures, and reconstructing table rows.
     */
    fun parsePdfStream(inputStream: InputStream): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        try {
            val bytes = inputStream.readBytes()
            val textBlocks = extractTextBlocksFromPdf(bytes)

            // 1. Try line-based table row extraction
            for (line in textBlocks) {
                val trimmed = line.trim()
                if (trimmed.isBlank()) continue

                val cols = when {
                    trimmed.contains(",") -> parseCsvLine(trimmed)
                    trimmed.contains("\t") -> trimmed.split("\t").map { it.trim() }
                    trimmed.contains("  ") -> trimmed.split(Regex("\\s{2,}")).map { it.trim() }
                    else -> emptyList()
                }

                if (cols.size >= 2 && !isSummaryOrHeaderRow(cols)) {
                    rows.add(cols)
                }
            }

            // 2. If line splitting produced insufficient rows (e.g. absolute positioned PDF),
            // extract atomic tokens and group into transactions starting at dates
            if (rows.size <= 1) {
                val tokenRows = extractTabularRowsFromTokens(textBlocks)
                if (tokenRows.isNotEmpty()) {
                    rows.clear()
                    rows.addAll(tokenRows)
                }
            }
        } catch (_: Exception) {
        }
        return rows
    }

    private fun extractTextBlocksFromPdf(bytes: ByteArray): List<String> {
        val blocks = mutableListOf<String>()
        val streams = extractAllStreams(bytes)

        for (stream in streams) {
            val streamLines = extractTextFromPdfStream(stream)
            blocks.addAll(streamLines)
        }

        // Also check uncompressed text in the root file for uncompressed PDFs
        val rawIso = String(bytes, StandardCharsets.ISO_8859_1)
        val plainTextLines = extractTextFromPdfStream(rawIso)
        if (blocks.isEmpty() && plainTextLines.isNotEmpty()) {
            blocks.addAll(plainTextLines)
        }

        return blocks
    }

    private fun extractAllStreams(bytes: ByteArray): List<String> {
        val streamContents = mutableListOf<String>()
        var offset = 0
        val len = bytes.size
        val streamTag = "stream".toByteArray(StandardCharsets.US_ASCII)
        val endStreamTag = "endstream".toByteArray(StandardCharsets.US_ASCII)

        while (offset < len) {
            val streamIdx = findByteSequence(bytes, offset, streamTag)
            if (streamIdx == -1) break

            var contentStart = streamIdx + 6
            if (contentStart < len && bytes[contentStart] == '\r'.code.toByte()) contentStart++
            if (contentStart < len && bytes[contentStart] == '\n'.code.toByte()) contentStart++

            val endStreamIdx = findByteSequence(bytes, contentStart, endStreamTag)
            if (endStreamIdx == -1) break

            var contentEnd = endStreamIdx
            if (contentEnd > contentStart && bytes[contentEnd - 1] == '\n'.code.toByte()) contentEnd--
            if (contentEnd > contentStart && bytes[contentEnd - 1] == '\r'.code.toByte()) contentEnd--

            val dictStart = max(0, streamIdx - 300)
            val dictString = String(bytes, dictStart, streamIdx - dictStart, StandardCharsets.ISO_8859_1)
            val isFlate = dictString.contains("/FlateDecode") || dictString.contains("/Fl")

            val streamBytes = bytes.copyOfRange(contentStart, contentEnd)
            var decompressedString: String? = null

            if (isFlate) {
                decompressedString = decompressZlib(streamBytes)
            }

            if (decompressedString == null) {
                decompressedString = try {
                    String(streamBytes, StandardCharsets.UTF_8)
                } catch (_: Exception) {
                    String(streamBytes, StandardCharsets.ISO_8859_1)
                }
            }

            if (decompressedString.isNotBlank()) {
                streamContents.add(decompressedString)
            }

            offset = endStreamIdx + 9
        }
        return streamContents
    }

    private fun decompressZlib(data: ByteArray): String? {
        // Try standard zlib
        try {
            val inflater = Inflater(false)
            inflater.setInput(data)
            val baos = ByteArrayOutputStream()
            val buf = ByteArray(4096)
            while (!inflater.finished() && !inflater.needsInput()) {
                val count = inflater.inflate(buf)
                if (count <= 0) break
                baos.write(buf, 0, count)
            }
            inflater.end()
            if (baos.size() > 0) {
                return String(baos.toByteArray(), StandardCharsets.UTF_8)
            }
        } catch (_: Exception) {
        }

        // Try raw deflate (nowrap)
        try {
            val inflater = Inflater(true)
            inflater.setInput(data)
            val baos = ByteArrayOutputStream()
            val buf = ByteArray(4096)
            while (!inflater.finished() && !inflater.needsInput()) {
                val count = inflater.inflate(buf)
                if (count <= 0) break
                baos.write(buf, 0, count)
            }
            inflater.end()
            if (baos.size() > 0) {
                return String(baos.toByteArray(), StandardCharsets.UTF_8)
            }
        } catch (_: Exception) {
        }
        return null
    }

    private fun findByteSequence(source: ByteArray, start: Int, target: ByteArray): Int {
        if (target.isEmpty() || start + target.size > source.size) return -1
        val maxIndex = source.size - target.size
        for (i in start..maxIndex) {
            var match = true
            for (j in target.indices) {
                if (source[i + j] != target[j]) {
                    match = false
                    break
                }
            }
            if (match) return i
        }
        return -1
    }

    private fun extractTextFromPdfStream(content: String): List<String> {
        val lines = mutableListOf<String>()
        val currentLine = StringBuilder()
        var i = 0
        val len = content.length

        while (i < len) {
            val c = content[i]
            when {
                c == '(' -> {
                    // Literal string
                    val strSb = StringBuilder()
                    i++
                    var escaped = false
                    var parenDepth = 1
                    while (i < len && parenDepth > 0) {
                        val sc = content[i]
                        if (escaped) {
                            when (sc) {
                                'n' -> strSb.append('\n')
                                'r' -> strSb.append('\r')
                                't' -> strSb.append('\t')
                                else -> strSb.append(sc)
                            }
                            escaped = false
                        } else if (sc == '\\') {
                            escaped = true
                        } else if (sc == '(') {
                            parenDepth++
                            strSb.append(sc)
                        } else if (sc == ')') {
                            parenDepth--
                            if (parenDepth > 0) strSb.append(sc)
                        } else {
                            strSb.append(sc)
                        }
                        i++
                    }
                    val textVal = strSb.toString().trim()
                    if (textVal.isNotEmpty()) {
                        if (currentLine.isNotEmpty()) currentLine.append("  ")
                        currentLine.append(textVal)
                    }
                    continue
                }
                c == '<' && i + 1 < len && content[i + 1] != '<' -> {
                    // Hex string
                    val hexSb = StringBuilder()
                    i++
                    while (i < len && content[i] != '>') {
                        if (!content[i].isWhitespace()) {
                            hexSb.append(content[i])
                        }
                        i++
                    }
                    val hexStr = hexSb.toString()
                    val decoded = decodePdfHexString(hexStr)
                    if (decoded.isNotBlank()) {
                        if (currentLine.isNotEmpty()) currentLine.append("  ")
                        currentLine.append(decoded)
                    }
                }
                c == '\n' || c == '\r' -> {
                    if (currentLine.isNotEmpty()) {
                        lines.add(currentLine.toString().trim())
                        currentLine.setLength(0)
                    }
                }
            }
            i++
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString().trim())
        }
        return lines
    }

    private fun decodePdfHexString(hex: String): String {
        if (hex.length < 2) return ""
        try {
            // Check UTF-16BE
            if (hex.length % 4 == 0) {
                val sb = StringBuilder()
                var isUtf16 = false
                var i = 0
                while (i < hex.length) {
                    val code = hex.substring(i, i + 4).toIntOrNull(16) ?: break
                    if (code in 0x0020..0x007E || code in 0x0980..0x09FF) {
                        isUtf16 = true
                        sb.append(code.toChar())
                    } else if (code == 0xFEFF) {
                        isUtf16 = true
                    } else {
                        break
                    }
                    i += 4
                }
                if (isUtf16 && sb.isNotEmpty()) return sb.toString()
            }

            // Fallback: ASCII / Latin-1 hex
            val bytes = ByteArray(hex.length / 2)
            for (j in 0 until hex.length / 2) {
                val b = hex.substring(j * 2, j * 2 + 2).toIntOrNull(16) ?: return ""
                bytes[j] = b.toByte()
            }
            return String(bytes, StandardCharsets.UTF_8)
        } catch (_: Exception) {
            return ""
        }
    }

    private fun extractTabularRowsFromTokens(textBlocks: List<String>): List<List<String>> {
        val allTokens = mutableListOf<String>()
        for (block in textBlocks) {
            // Split by 2+ spaces, tabs, or newlines
            val parts = block.split(Regex("[\\t\\n\\r]+|\\s{2,}"))
                .map { it.trim() }
                .filter { it.isNotBlank() }
            allTokens.addAll(parts)
        }

        val rows = mutableListOf<List<String>>()
        var curTokens = mutableListOf<String>()

        for (token in allTokens) {
            val dateParsed = parseDate(token)
            if (dateParsed != null && curTokens.isNotEmpty()) {
                val assembledRow = buildRowFromTokens(curTokens)
                if (assembledRow != null && !isSummaryOrHeaderRow(assembledRow)) {
                    rows.add(assembledRow)
                }
                curTokens = mutableListOf(token)
            } else {
                curTokens.add(token)
            }
        }
        if (curTokens.isNotEmpty()) {
            val assembledRow = buildRowFromTokens(curTokens)
            if (assembledRow != null && !isSummaryOrHeaderRow(assembledRow)) {
                rows.add(assembledRow)
            }
        }
        return rows
    }

    private fun buildRowFromTokens(tokens: List<String>): List<String>? {
        if (tokens.isEmpty()) return null
        val dateToken = tokens.firstOrNull() ?: return null
        if (parseDate(dateToken) == null) return null

        // Summary check
        val text = tokens.joinToString(" ").lowercase()
        if (text.contains("total income") || text.contains("total expense") || text.contains("net balance")) return null

        return when {
            tokens.size >= 6 -> {
                // [Date, Type, Category, Account, Note, Amount]
                listOf(tokens[0], tokens[1], tokens[2], tokens[3], tokens.subList(4, tokens.size - 1).joinToString(" "), tokens.last())
            }
            tokens.size == 5 -> {
                // [Date, Type, Category, Account, Amount]
                listOf(tokens[0], tokens[1], tokens[2], tokens[3], "", tokens[4])
            }
            tokens.size == 4 -> {
                // [Date, Category, Account, Amount]
                listOf(tokens[0], "Expense", tokens[1], tokens[2], "", tokens[3])
            }
            tokens.size == 3 -> {
                // [Date, Category, Amount]
                listOf(tokens[0], "Expense", tokens[1], "CASH", "", tokens[2])
            }
            else -> null
        }
    }

    suspend fun executeImport(
        context: Context,
        analysis: ImportAnalysis,
        skipDuplicates: Boolean,
        dao: TransactionDao
    ): ImportResult = withContext(Dispatchers.IO) {
        val categoryManager = CategoryManager.getInstance(context)

        // 1. Auto-create new detected custom categories
        var createdCats = 0
        for (catName in analysis.newCategories) {
            val sampleTx = analysis.validRows.firstOrNull { it.category.equals(catName, ignoreCase = true) }
            val catType = sampleTx?.type ?: TransactionType.EXPENSE
            if (!categoryManager.isDuplicateName(catName, catType)) {
                categoryManager.addCategory(
                    name = catName,
                    type = catType
                )
                createdCats++
            }
        }

        // 2. Select rows to import
        val rowsToImport = if (skipDuplicates) {
            analysis.validRows
        } else {
            analysis.validRows + analysis.duplicateRows
        }

        val entities = rowsToImport.map { row ->
            TransactionEntity(
                id = UUID.randomUUID().toString(),
                type = row.type.value,
                amount = row.amount,
                category = row.category,
                accountCode = row.accountCode,
                toAccountCode = if (row.type == TransactionType.TRANSFER) (row.toAccountCode ?: AccountType.BKASH.code) else null,
                date = row.date,
                description = row.note,
                createdAt = System.currentTimeMillis(),
                isSynced = false,
                syncStatus = "PENDING_CREATE"
            )
        }

        if (entities.isNotEmpty()) {
            dao.insertAll(entities)
        }

        ImportResult(
            totalRows = analysis.totalRowsCount,
            importedCount = entities.size,
            skippedDuplicates = if (skipDuplicates) analysis.duplicateRows.size else 0,
            skippedErrors = analysis.invalidRows.size,
            newCategoriesCreated = createdCats
        )
    }
}
