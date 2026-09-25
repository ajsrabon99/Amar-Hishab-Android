package com.ajyra.amarhishab.data.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.ajyra.amarhishab.BuildConfig
import com.ajyra.amarhishab.data.local.CategoryManager
import com.ajyra.amarhishab.data.local.EncryptedSessionManager
import com.ajyra.amarhishab.data.local.SavingsGoalDao
import com.ajyra.amarhishab.data.local.SavingsGoalEntity
import com.ajyra.amarhishab.data.local.TransactionDao
import com.ajyra.amarhishab.data.local.TransactionEntity
import com.ajyra.amarhishab.model.CategoryItem
import com.ajyra.amarhishab.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupAnalysis(
    val isValid: Boolean,
    val transactionCount: Int = 0,
    val categoryCount: Int = 0,
    val goalCount: Int = 0,
    val backupDate: String = "",
    val appVersion: String = "",
    val errorReason: String? = null
)

data class RestoreResult(
    val success: Boolean,
    val transactionsRestored: Int = 0,
    val categoriesRestored: Int = 0,
    val goalsRestored: Int = 0,
    val errorMessage: String? = null
)

object DataBackupService {

    private const val PREFS_BACKUP = "amar_hishab_backup_prefs"
    private const val KEY_LAST_BACKUP = "key_last_backup_timestamp"
    private const val KEY_AUTO_BACKUP = "key_auto_backup_enabled"

    fun getLastBackupTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_BACKUP, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_BACKUP, 0L)
    }

    fun isAutoBackupEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_BACKUP, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_BACKUP, false)
    }

    fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_BACKUP, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_BACKUP, enabled).apply()
    }

    private fun setLastBackupTime(context: Context, timestamp: Long) {
        val prefs = context.getSharedPreferences(PREFS_BACKUP, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_BACKUP, timestamp).apply()
    }

    suspend fun createBackup(
        context: Context,
        dao: TransactionDao,
        goalDao: SavingsGoalDao
    ): File = withContext(Dispatchers.IO) {
        val dateStamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val fileName = "Amar-Hishab-Backup-$dateStamp.zip"
        val zipFile = File(context.cacheDir, fileName)

        // 1. Transactions JSON
        val transactions = dao.getAllTransactionsList()
        val txArray = JSONArray()
        for (tx in transactions) {
            val obj = JSONObject().apply {
                put("id", tx.id)
                put("type", tx.type)
                put("amount", tx.amount)
                put("category", tx.category)
                put("accountCode", tx.accountCode)
                put("toAccountCode", tx.toAccountCode ?: "")
                put("date", tx.date)
                put("description", tx.description ?: "")
                put("createdAt", tx.createdAt)
            }
            txArray.put(obj)
        }

        // 2. Custom Categories JSON
        val categoryManager = CategoryManager.getInstance(context)
        val categories: List<CategoryItem> = categoryManager.customCategories.value
        val catArray = JSONArray()
        for (cat in categories) {
            val obj = JSONObject().apply {
                put("id", cat.id)
                put("nameEn", cat.nameEn)
                put("nameBn", cat.nameBn)
                put("type", cat.type.value)
                put("iconName", cat.iconName)
            }
            catArray.put(obj)
        }

        // 3. Savings Goals JSON
        val goals = goalDao.getAllGoalsList()
        val goalArray = JSONArray()
        for (g in goals) {
            val obj = JSONObject().apply {
                put("id", g.id)
                put("name", g.name)
                put("targetAmount", g.targetAmount)
                put("savedAmount", g.savedAmount)
                put("targetDate", g.targetDate)
                put("description", g.description ?: "")
                put("iconCategory", g.iconCategory ?: "")
                put("isActiveOnDashboard", g.isActiveOnDashboard)
                put("createdAt", g.createdAt)
            }
            goalArray.put(obj)
        }

        // 4. Accounts & Settings JSON
        val session = EncryptedSessionManager.getInstance(context)
        val settingsObj = JSONObject().apply {
            put("language", session.getSavedLanguage())
            put("themeMode", session.getSavedTheme())
            put("appLockEnabled", session.isAppLockEnabled())
            put("notificationsEnabled", session.areNotificationsEnabled())
        }

        // 5. Metadata JSON
        val metaObj = JSONObject().apply {
            put("appName", "Amar Hishab")
            put("appVersion", BuildConfig.VERSION_NAME)
            put("versionCode", BuildConfig.VERSION_CODE)
            put("backupFormatVersion", "1.0")
            put("backupTimestamp", System.currentTimeMillis())
            put("backupDateFormatted", SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.US).format(Date()))
            put("transactionCount", transactions.size)
            put("categoryCount", categories.size)
            put("goalCount", goals.size)
        }

        // Write Zip
        FileOutputStream(zipFile).use { fos ->
            ZipOutputStream(fos).use { zos ->
                fun writeZipEntry(name: String, content: String) {
                    val entry = ZipEntry(name)
                    zos.putNextEntry(entry)
                    zos.write(content.toByteArray(StandardCharsets.UTF_8))
                    zos.closeEntry()
                }

                writeZipEntry("backup_metadata.json", metaObj.toString(2))
                writeZipEntry("transactions.json", txArray.toString(2))
                writeZipEntry("categories.json", catArray.toString(2))
                writeZipEntry("goals.json", goalArray.toString(2))
                writeZipEntry("settings.json", settingsObj.toString(2))
            }
        }

        setLastBackupTime(context, System.currentTimeMillis())
        zipFile
    }

    fun shareBackupFile(context: Context, file: File, isBengali: Boolean) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, if (isBengali) "আমার হিসাব ডেটা ব্যাকআপ" else "Amar Hishab Data Backup")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooserTitle = if (isBengali) "ব্যাকআপ ফাইল সংরক্ষণ বা শেয়ার করুন" else "Save or Share Amar Hishab Backup"
        context.startActivity(Intent.createChooser(sendIntent, chooserTitle))
    }

    suspend fun analyzeBackup(context: Context, uri: Uri): BackupAnalysis = withContext(Dispatchers.IO) {
        try {
            val filesMap = mutableMapOf<String, String>()
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val zis = ZipInputStream(stream)
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val name = entry.name.lowercase()
                        val baos = ByteArrayOutputStream()
                        zis.copyTo(baos)
                        filesMap[name] = String(baos.toByteArray(), StandardCharsets.UTF_8)
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            val metaJson = filesMap["backup_metadata.json"]
            if (metaJson == null && !filesMap.containsKey("transactions.json")) {
                return@withContext BackupAnalysis(
                    isValid = false,
                    errorReason = "Not a valid Amar Hishab backup file (missing backup_metadata.json or transactions.json)"
                )
            }

            var txCount = 0
            var catCount = 0
            var goalCount = 0
            var dateStr = "Unknown"
            var verStr = "v1.1.1"

            if (metaJson != null) {
                val meta = JSONObject(metaJson)
                txCount = meta.optInt("transactionCount", 0)
                catCount = meta.optInt("categoryCount", 0)
                goalCount = meta.optInt("goalCount", 0)
                dateStr = meta.optString("backupDateFormatted", "Unknown")
                verStr = meta.optString("appVersion", "1.1.1")
            }

            filesMap["transactions.json"]?.let {
                try {
                    val arr = JSONArray(it)
                    txCount = arr.length()
                } catch (_: Exception) {}
            }
            filesMap["categories.json"]?.let {
                try {
                    val arr = JSONArray(it)
                    catCount = arr.length()
                } catch (_: Exception) {}
            }
            filesMap["goals.json"]?.let {
                try {
                    val arr = JSONArray(it)
                    goalCount = arr.length()
                } catch (_: Exception) {}
            }

            BackupAnalysis(
                isValid = true,
                transactionCount = txCount,
                categoryCount = catCount,
                goalCount = goalCount,
                backupDate = dateStr,
                appVersion = verStr
            )
        } catch (e: Exception) {
            BackupAnalysis(
                isValid = false,
                errorReason = "Failed to parse backup ZIP: ${e.message}"
            )
        }
    }

    suspend fun restoreBackup(
        context: Context,
        uri: Uri,
        dao: TransactionDao,
        goalDao: SavingsGoalDao
    ): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val filesMap = mutableMapOf<String, String>()
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val zis = ZipInputStream(stream)
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val name = entry.name.lowercase()
                        val baos = ByteArrayOutputStream()
                        zis.copyTo(baos)
                        filesMap[name] = String(baos.toByteArray(), StandardCharsets.UTF_8)
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            var restoredTx = 0
            var restoredCat = 0
            var restoredGoals = 0

            // 1. Restore Custom Categories
            filesMap["categories.json"]?.let { jsonStr ->
                val categoryManager = CategoryManager.getInstance(context)
                val catArray = JSONArray(jsonStr)
                for (i in 0 until catArray.length()) {
                    val obj = catArray.getJSONObject(i)
                    val nameEn = obj.optString("nameEn", "")
                    val nameBn = obj.optString("nameBn", "")
                    val typeStr = obj.optString("type", "EXPENSE")
                    val type = if (typeStr.equals("INCOME", ignoreCase = true)) TransactionType.INCOME else TransactionType.EXPENSE
                    val targetName = nameEn.ifBlank { nameBn }
                    if (targetName.isNotBlank() && !categoryManager.isDuplicateName(targetName, type)) {
                        categoryManager.addCategory(
                            name = targetName,
                            type = type
                        )
                        restoredCat++
                    }
                }
            }

            // 2. Restore Transactions
            filesMap["transactions.json"]?.let { jsonStr ->
                val existing = dao.getAllTransactionsList()
                val existingKeys = existing.map {
                    "${it.date.trim()}_${String.format(Locale.US, "%.2f", it.amount)}_${it.type.trim().uppercase()}_${it.category.trim().lowercase()}"
                }.toSet()

                val txArray = JSONArray(jsonStr)
                val toInsert = mutableListOf<TransactionEntity>()

                for (i in 0 until txArray.length()) {
                    val obj = txArray.getJSONObject(i)
                    val date = obj.getString("date")
                    val amount = obj.getDouble("amount")
                    val type = obj.getString("type")
                    val category = obj.getString("category")
                    val key = "${date.trim()}_${String.format(Locale.US, "%.2f", amount)}_${type.trim().uppercase()}_${category.trim().lowercase()}"

                    if (!existingKeys.contains(key)) {
                        toInsert.add(
                            TransactionEntity(
                                id = obj.optString("id").takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
                                type = type,
                                amount = amount,
                                category = category,
                                accountCode = obj.getString("accountCode"),
                                toAccountCode = obj.optString("toAccountCode").takeIf { it.isNotBlank() },
                                date = date,
                                description = obj.optString("description").takeIf { it.isNotBlank() },
                                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                                isSynced = false,
                                syncStatus = "PENDING_CREATE"
                            )
                        )
                    }
                }

                if (toInsert.isNotEmpty()) {
                    dao.insertAll(toInsert)
                    restoredTx = toInsert.size
                }
            }

            // 3. Restore Savings Goals
            filesMap["goals.json"]?.let { jsonStr ->
                val existingGoals = goalDao.getAllGoalsList().map { it.name.lowercase().trim() }.toSet()
                val goalArray = JSONArray(jsonStr)
                val toInsertGoals = mutableListOf<SavingsGoalEntity>()

                for (i in 0 until goalArray.length()) {
                    val obj = goalArray.getJSONObject(i)
                    val name = obj.getString("name")
                    if (!existingGoals.contains(name.lowercase().trim())) {
                        toInsertGoals.add(
                            SavingsGoalEntity(
                                id = obj.optString("id").takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
                                name = name,
                                targetAmount = obj.getDouble("targetAmount"),
                                savedAmount = obj.optDouble("savedAmount", 0.0),
                                targetDate = obj.getString("targetDate"),
                                description = obj.optString("description").takeIf { it.isNotBlank() },
                                iconCategory = obj.optString("iconCategory").takeIf { it.isNotBlank() } ?: obj.optString("category").takeIf { it.isNotBlank() },
                                isActiveOnDashboard = obj.optBoolean("isActiveOnDashboard", false),
                                createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                            )
                        )
                    }
                }

                if (toInsertGoals.isNotEmpty()) {
                    goalDao.insertAll(toInsertGoals)
                    restoredGoals = toInsertGoals.size
                }
            }

            RestoreResult(
                success = true,
                transactionsRestored = restoredTx,
                categoriesRestored = restoredCat,
                goalsRestored = restoredGoals
            )
        } catch (e: Exception) {
            RestoreResult(
                success = false,
                errorMessage = e.message ?: "Failed to restore backup"
            )
        }
    }
}
