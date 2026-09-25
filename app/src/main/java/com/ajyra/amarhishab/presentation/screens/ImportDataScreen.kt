package com.ajyra.amarhishab.presentation.screens

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajyra.amarhishab.data.local.AmarHishabDatabase
import com.ajyra.amarhishab.data.service.ColumnMapping
import com.ajyra.amarhishab.data.service.DataImportService
import com.ajyra.amarhishab.data.service.ImportAnalysis
import com.ajyra.amarhishab.data.service.ImportResult
import com.ajyra.amarhishab.model.TransactionType
import com.ajyra.amarhishab.ui.theme.EmeraldPrimary
import com.ajyra.amarhishab.ui.theme.ExpenseRed
import com.ajyra.amarhishab.ui.theme.IncomeGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportDataScreen(
    isBengali: Boolean,
    onNavigateBack: () -> Unit,
    onImportSuccess: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { AmarHishabDatabase.getInstance(context).transactionDao() }

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }

    var analysisResult by remember { mutableStateOf<ImportAnalysis?>(null) }
    var skipDuplicates by remember { mutableStateOf(true) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<ImportResult?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            var name = "import_file"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
            selectedFileName = name

            // Run analysis
            scope.launch {
                isAnalyzing = true
                analysisResult = null
                importResult = null
                try {
                    val analysis = DataImportService.analyzeFile(
                        context = context,
                        uri = uri,
                        fileName = name,
                        dao = dao
                    )
                    analysisResult = analysis
                } catch (e: Exception) {
                    Toast.makeText(context, "Error analyzing file: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isAnalyzing = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isBengali) "আর্থিক ডেটা ইমপোর্ট" else "Import Financial Data",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Step 1: File Picker Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, tint = EmeraldPrimary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isBengali) "ধাপ ১: ফাইল নির্বাচন করুন" else "Step 1: Select Data File",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isBengali) "CSV, ZIP, Excel (.xlsx) অথবা PDF স্টেটমেন্ট সমর্থন করে" else "Supports CSV, ZIP, Excel (.xlsx), and Statement PDF",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (selectedFileName != null) {
                        val fileIcon = when {
                            selectedFileName?.endsWith(".zip", ignoreCase = true) == true -> Icons.Default.FolderZip
                            selectedFileName?.endsWith(".xlsx", ignoreCase = true) == true || selectedFileName?.endsWith(".xls", ignoreCase = true) == true -> Icons.Default.TableChart
                            selectedFileName?.endsWith(".pdf", ignoreCase = true) == true -> Icons.Default.PictureAsPdf
                            else -> Icons.Default.InsertDriveFile
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = fileIcon,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = selectedFileName ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            filePickerLauncher.launch(
                                arrayOf(
                                    "text/csv",
                                    "text/comma-separated-values",
                                    "application/csv",
                                    "text/plain",
                                    "application/zip",
                                    "application/x-zip-compressed",
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "application/vnd.ms-excel",
                                    "application/pdf",
                                    "*/*"
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth().testTag("pick_import_file_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.FileUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (selectedFileName == null) (if (isBengali) "ফাইল বাছুন (CSV / ZIP / XLSX / PDF)" else "Choose File (CSV / ZIP / XLSX / PDF)") else (if (isBengali) "অন্য ফাইল বাছুন" else "Change File"))
                    }
                }
            }

            // Loading state
            if (isAnalyzing) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = if (isBengali) "ফাইল বিশ্লেষণ ও কলাম শনাক্ত করা হচ্ছে..." else "Analyzing file & detecting columns...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Step 2 & 3: Analysis, Column Mapping & Preview
            analysisResult?.let { analysis ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = if (isBengali) "ধাপ ২: বিশ্লেষণ ও কলাম ম্যাপিং" else "Step 2: Analysis & Mapping",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (analysis.detectedFilesInsideZip.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EmeraldPrimary.copy(alpha = 0.1f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = if (isBengali) "ZIP ফাইলে প্রাপ্ত ডেটা ফাইল:" else "Data files detected inside ZIP:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    analysis.detectedFilesInsideZip.forEach { f ->
                                        Text("• $f", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }

                        // Column Mapping display
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = if (isBengali) "শনাক্তকৃত কলাম:" else "Detected Columns & Mapping:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            analysis.rawHeaders.forEachIndexed { idx, header ->
                                val mappedField = when (idx) {
                                    analysis.detectedMapping.dateColIndex -> if (isBengali) "তারিখ (Date)" else "Date"
                                    analysis.detectedMapping.amountColIndex -> if (isBengali) "টাকার পরিমাণ (Amount)" else "Amount"
                                    analysis.detectedMapping.typeColIndex -> if (isBengali) "ধরন (Type)" else "Type"
                                    analysis.detectedMapping.categoryColIndex -> if (isBengali) "ক্যাটাগরি (Category)" else "Category"
                                    analysis.detectedMapping.accountColIndex -> if (isBengali) "অ্যাকাউন্ট (Account)" else "Account"
                                    analysis.detectedMapping.noteColIndex -> if (isBengali) "বিবরণ (Note)" else "Note"
                                    else -> if (isBengali) "বাদ দেওয়া হবে (Ignored)" else "Ignored"
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(header, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                    Text("➔ $mappedField", style = MaterialTheme.typography.bodySmall, color = EmeraldPrimary, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        // Step 4: Verification / Validation Stats
                        Text(
                            text = if (isBengali) "ধাপ ৩: ডেটা যাচাই ও প্রিভিউ" else "Step 3: Verification & Preview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Total records
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(if (isBengali) "মোট রেকর্ড" else "Total", style = MaterialTheme.typography.labelSmall)
                                    Text("${analysis.totalRowsCount}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                }
                            }

                            // Valid
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = IncomeGreen.copy(alpha = 0.12f))
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(if (isBengali) "সঠিক" else "Valid", style = MaterialTheme.typography.labelSmall, color = IncomeGreen)
                                    Text("${analysis.validRows.size}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = IncomeGreen)
                                }
                            }

                            // Duplicates
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = if (analysis.duplicateRows.isNotEmpty()) Color(0xFFFFF3CD) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(if (isBengali) "ডুপ্লিকেট" else "Duplicates", style = MaterialTheme.typography.labelSmall, color = Color(0xFF856404))
                                    Text("${analysis.duplicateRows.size}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = Color(0xFF856404))
                                }
                            }

                            // Errors
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = if (analysis.invalidRows.isNotEmpty()) ExpenseRed.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(if (isBengali) "ত্রুটি" else "Errors", style = MaterialTheme.typography.labelSmall, color = ExpenseRed)
                                    Text("${analysis.invalidRows.size}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = ExpenseRed)
                                }
                            }
                        }

                        // Duplicate handling option
                        if (analysis.duplicateRows.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { skipDuplicates = !skipDuplicates }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = skipDuplicates,
                                    onCheckedChange = { skipDuplicates = it }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = if (isBengali) "পূর্বের ডুপ্লিকেট লেনদেন বাদ দিন (সুপারিশকৃত)" else "Skip duplicate transactions (Recommended)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (isBengali) "একই তারিখ, পরিমাণ ও খাতের লেনদেন পুনরায় যুক্ত হবে না" else "Transactions matching existing date, amount and category won't be duplicated",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Detected new categories
                        if (analysis.newCategories.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = if (isBengali) "নতুন ক্যাটাগরি শনাক্ত হয়েছে (${analysis.newCategories.size} টি):" else "New categories detected (${analysis.newCategories.size}):",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = analysis.newCategories.joinToString(", "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = EmeraldPrimary
                                    )
                                    Text(
                                        text = if (isBengali) "ইমপোর্ট নিশ্চিত করলে এগুলি স্বয়ংক্রিয়ভাবে কাস্টম খাত হিসেবে তৈরি হবে।" else "These will be automatically created as custom categories upon confirmation.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Sample Preview Rows
                        if (analysis.sampleRows.isNotEmpty()) {
                            Text(
                                text = if (isBengali) "নমুনা ডেটা প্রিভিউ:" else "Sample Records Preview:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                analysis.sampleRows.forEach { row ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (row.isDuplicate) Color(0xFFFFF9E6) else if (!row.isValid) ExpenseRed.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "${row.date} • ${row.category}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                if (row.note != null) {
                                                    Text(row.note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                if (row.errorReason != null) {
                                                    Text(row.errorReason, style = MaterialTheme.typography.labelSmall, color = ExpenseRed)
                                                }
                                            }
                                            Text(
                                                text = "${if (row.type == TransactionType.INCOME) "+" else "-"} ৳${row.amount}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (row.type == TransactionType.INCOME) IncomeGreen else ExpenseRed
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Import Action Button
                        Button(
                            onClick = { showConfirmDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("confirm_import_button"),
                            enabled = analysis.validRows.isNotEmpty() && !isImporting,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            if (isImporting) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                val toImportCount = if (skipDuplicates) analysis.validRows.size else (analysis.validRows.size + analysis.duplicateRows.size)
                                Text(
                                    text = if (isBengali) "$toImportCount টি লেনদেন ইমপোর্ট করুন" else "Import $toImportCount Transactions",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Step 5: Final Result Summary
            importResult?.let { res ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = IncomeGreen.copy(alpha = 0.12f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = IncomeGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBengali) "ইমপোর্ট সফলভাবে সম্পন্ন হয়েছে!" else "Import Completed Successfully!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                        }

                        Text("• ${if (isBengali) "সফলভাবে ইমপোর্ট হয়েছে:" else "Successfully Imported:"} ${res.importedCount} ${if (isBengali) "টি" else "records"}")
                        if (res.skippedDuplicates > 0) {
                            Text("• ${if (isBengali) "ডুপ্লিকেট হওয়ায় বাদ দেওয়া হয়েছে:" else "Skipped Duplicates:"} ${res.skippedDuplicates} ${if (isBengali) "টি" else "records"}")
                        }
                        if (res.skippedErrors > 0) {
                            Text("• ${if (isBengali) "ত্রুটিযুক্ত হওয়ায় বাদ দেওয়া হয়েছে:" else "Skipped Errors:"} ${res.skippedErrors} ${if (isBengali) "টি" else "records"}")
                        }
                        if (res.newCategoriesCreated > 0) {
                            Text("• ${if (isBengali) "নতুন কাস্টম খাত তৈরি হয়েছে:" else "New Categories Created:"} ${res.newCategoriesCreated} ${if (isBengali) "টি" else "categories"}")
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Button(
                            onClick = onNavigateBack,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text(if (isBengali) "ড্যাশবোর্ডে ফিরে যান" else "Return to Dashboard")
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog
    if (showConfirmDialog) {
        val analysis = analysisResult
        val count = if (analysis != null) {
            if (skipDuplicates) analysis.validRows.size else (analysis.validRows.size + analysis.duplicateRows.size)
        } else 0

        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    text = if (isBengali) "ইমপোর্ট নিশ্চিত করুন" else "Confirm Data Import",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isBengali)
                            "আপনি কি নিশ্চিত যে $count টি লেনদেন লোকাল ডেটাবেজে যুক্ত করতে চান? পূর্বের কোনো লেনদেন মুছে যাবে না।"
                        else
                            "Are you sure you want to import $count transactions into your local database? Existing transactions will remain completely intact."
                    )
                    if (skipDuplicates && (analysis?.duplicateRows?.size ?: 0) > 0) {
                        Text(
                            text = if (isBengali)
                                "(${analysis?.duplicateRows?.size} টি ডুপ্লিকেট লেনদেন বাদ দেওয়া হবে)"
                            else
                                "(${analysis?.duplicateRows?.size} duplicate records will be skipped)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        analysis?.let { curAnalysis ->
                            scope.launch {
                                isImporting = true
                                try {
                                    val res = DataImportService.executeImport(
                                        context = context,
                                        analysis = curAnalysis,
                                        skipDuplicates = skipDuplicates,
                                        dao = dao
                                    )
                                    importResult = res
                                    onImportSuccess?.invoke()
                                    Toast.makeText(
                                        context,
                                        if (isBengali) "${res.importedCount} টি লেনদেন যুক্ত হয়েছে" else "${res.importedCount} transactions imported",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isImporting = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(if (isBengali) "ইমপোর্ট নিশ্চিত করুন" else "Confirm Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(if (isBengali) "বাতিল" else "Cancel")
                }
            }
        )
    }
}
