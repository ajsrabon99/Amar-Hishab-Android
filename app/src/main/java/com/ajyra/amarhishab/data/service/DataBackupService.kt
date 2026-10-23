package com.ajyra.amarhishab.data.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.ajyra.amarhishab.BuildConfig
import com.ajyra.amarhishab.data.local.AmarHishabDatabase
import com.ajyra.amarhishab.data.local.CategoryManager
import com.ajyra.amarhishab.data.local.EncryptedSessionManager
import com.ajyra.amarhishab.data.local.SavingsGoalDao
import com.ajyra.amarhishab.data.local.SavingsGoalEntity
import com.ajyra.amarhishab.data.local.SpendingPlannerManager
import com.ajyra.amarhishab.data.local.TransactionDao
import com.ajyra.amarhishab.data.local.TransactionEntity
import com.ajyra.amarhishab.model.CategoryItem
import com.ajyra.amarhishab.model.TransactionType
import com.ajyra.amarhishab.model.Workspace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
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
    val personalTransactionCount: Int = 0,
    val businessTransactionCount: Int = 0,
    val categoryCount: Int = 0,
    val goalCount: Int = 0,
    val hasProfileData: Boolean = false,
    val hasProfilePhoto: Boolean = false,
    val hasSettings: Boolean = false,
    val hasSmartSpending: Boolean = false,
    val profileName: String = "",
    val backupDate: String = "",
    val appVersion: String = "",
    val errorReason: String? = null
)

data class RestoreResult(
    val success: Boolean,
    val transactionsRestored: Int = 0,
    val personalTransactionsRestored: Int = 0,
    val businessTransactionsRestored: Int = 0,
    val categoriesRestored: Int = 0,
    val goalsRestored: Int = 0,
    val profileRestored: Boolean = false,
    val settingsRestored: Boolean = false,
    val smartSpendingRestored: Boolean = false,
    val budgetsRestored: Int = 0,
    val recurringRestored: Int = 0,
    val remindersRestored: Int = 0,
    val errorMessage: String? = null
)

object DataBackupService {

    private const val TAG = "DataBackupService"
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
        goalDao: SavingsGoalDao,
        customOutputFile: File? = null
    ): File = withContext(Dispatchers.IO) {
        val dateStamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())
        val fileName = "Amar-Hishab-Backup-$dateStamp.zip"
        val zipFile = customOutputFile ?: File(context.cacheDir, fileName)

        // 1. Transactions JSON (Personal, Business, Incomes, Expenses, Transfers)
        val transactions = dao.getAllTransactionsList()
        val txArray = JSONArray()
        var personalCount = 0
        var businessCount = 0

        for (tx in transactions) {
            val ws = if (tx.workspace.equals("BUSINESS", ignoreCase = true)) "BUSINESS" else "PERSONAL"
            if (ws == "BUSINESS") businessCount++ else personalCount++

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
                put("workspace", ws)
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
                put("colorHex", cat.colorHex)
                put("workspace", cat.workspace)
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
                put("workspace", g.workspace)
            }
            goalArray.put(obj)
        }

        // 4. Smart Spending Settings JSON
        val plannerManager = SpendingPlannerManager.getInstance(context)
        val smartSpendingObj = JSONObject().apply {
            put("personalMonthlyLimit", plannerManager.getMonthlyLimit(Workspace.PERSONAL))
            put("businessMonthlyLimit", plannerManager.getMonthlyLimit(Workspace.BUSINESS))
            put("personalRecurringLimit", plannerManager.isRecurringLimitEnabled(Workspace.PERSONAL))
            put("businessRecurringLimit", plannerManager.isRecurringLimitEnabled(Workspace.BUSINESS))
        }

        // 5. User Profile JSON and actual profile photo extraction
        val session = EncryptedSessionManager.getInstance(context)
        val user = session.getSavedUser()
        var profilePhotoBytes: ByteArray? = null
        if (!user.avatarUri.isNullOrBlank()) {
            try {
                val avatarFile = File(user.avatarUri)
                if (avatarFile.exists() && avatarFile.canRead()) {
                    profilePhotoBytes = avatarFile.readBytes()
                } else {
                    val uri = Uri.parse(user.avatarUri)
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        profilePhotoBytes = stream.readBytes()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Profile photo read note: ${e.message}")
            }
        }

        val profileObj = JSONObject().apply {
            put("name", user.displayName)
            put("email", user.email)
            put("phone", user.phone)
            put("hasPhoto", profilePhotoBytes != null)
            put("photoEntryName", if (profilePhotoBytes != null) "profile_photo.jpg" else "")
        }

        // 6. User Preferences & Settings JSON (strictly non-sensitive, no tokens or passwords)
        val settingsObj = JSONObject().apply {
            put("language", session.getSavedLanguage())
            put("themeMode", session.getSavedTheme())
            put("activeWorkspace", session.getSavedWorkspace().id)
            put("isBalanceHidden", session.isBalanceHidden())
            put("appLockEnabled", session.isAppLockEnabled())
            put("notificationsEnabled", session.areNotificationsEnabled())
            put("expenseRemindersEnabled", session.areExpenseRemindersEnabled())
            put("monthlySummaryEnabled", session.isMonthlySummaryEnabled())
        }

        // 7. Metadata JSON
        val metaObj = JSONObject().apply {
            put("appName", "Amar Hishab")
            put("appVersion", BuildConfig.VERSION_NAME)
            put("versionCode", BuildConfig.VERSION_CODE)
            put("backupFormatVersion", "1.3")
            put("backupTimestamp", System.currentTimeMillis())
            put("backupDateFormatted", SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.US).format(Date()))
            put("transactionCount", transactions.size)
            put("personalTransactionCount", personalCount)
            put("businessTransactionCount", businessCount)
            put("categoryCount", categories.size)
            put("goalCount", goals.size)
            put("hasProfile", true)
            put("hasProfilePhoto", profilePhotoBytes != null)
            put("profileName", user.displayName)
            put("hasSettings", true)
            put("hasSmartSpending", true)
        }

        // 8. Universal compatibility entry (BackupManager format)
        val legacyObj = JSONObject().apply {
            put("version", 2)
            put("timestamp", System.currentTimeMillis())
            put("transactions", txArray)
            val accArr = JSONArray()
            val accounts = listOf("cash", "bkash", "nagad", "bank")
            accounts.forEach { acc ->
                accArr.put(JSONObject().apply {
                    put("id", acc)
                    put("name", acc)
                })
            }
            put("accounts", accArr)
        }

        // Write Zip File
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
                writeZipEntry("smart_spending.json", smartSpendingObj.toString(2))
                writeZipEntry("profile.json", profileObj.toString(2))
                writeZipEntry("settings.json", settingsObj.toString(2))
                writeZipEntry("amar_hishab_backup.json", legacyObj.toString(2))

                // Embed actual profile photo binary if present
                val photoBytesToWrite = profilePhotoBytes
                if (photoBytesToWrite != null && photoBytesToWrite.isNotEmpty()) {
                    val photoEntry = ZipEntry("profile_photo.jpg")
                    zos.putNextEntry(photoEntry)
                    zos.write(photoBytesToWrite)
                    zos.closeEntry()
                }
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
            putExtra(Intent.EXTRA_SUBJECT, if (isBengali) "আমার হিসাব সম্পূর্ণ ব্যাকআপ" else "Amar Hishab Complete Backup")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooserTitle = if (isBengali) "ব্যাকআপ ফাইল সংরক্ষণ বা শেয়ার করুন" else "Save or Share Amar Hishab Backup"
        context.startActivity(Intent.createChooser(sendIntent, chooserTitle))
    }

    suspend fun analyzeBackup(context: Context, uri: Uri): BackupAnalysis = withContext(Dispatchers.IO) {
        try {
            val stream = context.contentResolver.openInputStream(uri)
                ?: return@withContext BackupAnalysis(false, errorReason = "Unable to open backup file")
            analyzeBackupStream(stream)
        } catch (e: Exception) {
            BackupAnalysis(isValid = false, errorReason = "Failed to parse backup ZIP: ${e.message}")
        }
    }

    fun analyzeBackupStream(inputStream: InputStream): BackupAnalysis {
        return try {
            val textFilesMap = mutableMapOf<String, String>()
            var hasPhotoBinary = false

            ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val name = entry.name.lowercase().substringAfterLast('/')
                        val baos = ByteArrayOutputStream()
                        zis.copyTo(baos)
                        if (name == "profile_photo.jpg" || name.endsWith(".jpg") || name.endsWith(".png")) {
                            hasPhotoBinary = true
                        } else {
                            textFilesMap[name] = String(baos.toByteArray(), StandardCharsets.UTF_8)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            val metaJson = textFilesMap["backup_metadata.json"]
            val hasTx = textFilesMap.containsKey("transactions.json") || textFilesMap.containsKey("amar_hishab_backup.json")
            if (metaJson == null && !hasTx) {
                return BackupAnalysis(
                    isValid = false,
                    errorReason = "Not a valid Amar Hishab backup file (missing backup_metadata.json or transactions.json)"
                )
            }

            var txCount = 0
            var personalTx = 0
            var businessTx = 0
            var catCount = 0
            var goalCount = 0
            var hasProfile = textFilesMap.containsKey("profile.json")
            var hasSettings = textFilesMap.containsKey("settings.json")
            var hasSmartSpending = textFilesMap.containsKey("smart_spending.json")
            var profileName = ""
            var dateStr = "Unknown"
            var verStr = "v1.3.0"

            if (metaJson != null) {
                val meta = JSONObject(metaJson)
                txCount = meta.optInt("transactionCount", 0)
                personalTx = meta.optInt("personalTransactionCount", 0)
                businessTx = meta.optInt("businessTransactionCount", 0)
                catCount = meta.optInt("categoryCount", 0)
                goalCount = meta.optInt("goalCount", 0)
                hasProfile = meta.optBoolean("hasProfile", hasProfile)
                hasPhotoBinary = meta.optBoolean("hasProfilePhoto", hasPhotoBinary)
                profileName = meta.optString("profileName", "")
                hasSettings = meta.optBoolean("hasSettings", hasSettings)
                hasSmartSpending = meta.optBoolean("hasSmartSpending", hasSmartSpending)
                dateStr = meta.optString("backupDateFormatted", "Unknown")
                verStr = meta.optString("appVersion", "1.3.0")
            }

            textFilesMap["transactions.json"]?.let {
                try {
                    val arr = JSONArray(it)
                    txCount = arr.length()
                    var p = 0
                    var b = 0
                    for (i in 0 until arr.length()) {
                        val ws = arr.getJSONObject(i).optString("workspace", "PERSONAL")
                        if (ws.equals("BUSINESS", ignoreCase = true)) b++ else p++
                    }
                    personalTx = p
                    businessTx = b
                } catch (_: Exception) {}
            }
            textFilesMap["categories.json"]?.let {
                try {
                    val arr = JSONArray(it)
                    catCount = arr.length()
                } catch (_: Exception) {}
            }
            textFilesMap["goals.json"]?.let {
                try {
                    val arr = JSONArray(it)
                    goalCount = arr.length()
                } catch (_: Exception) {}
            }
            textFilesMap["profile.json"]?.let {
                try {
                    val obj = JSONObject(it)
                    profileName = obj.optString("name", profileName)
                    hasProfile = true
                    if (obj.optBoolean("hasPhoto", false)) {
                        hasPhotoBinary = true
                    }
                } catch (_: Exception) {}
            }

            BackupAnalysis(
                isValid = true,
                transactionCount = txCount,
                personalTransactionCount = personalTx,
                businessTransactionCount = businessTx,
                categoryCount = catCount,
                goalCount = goalCount,
                hasProfileData = hasProfile,
                hasProfilePhoto = hasPhotoBinary,
                hasSettings = hasSettings,
                hasSmartSpending = hasSmartSpending,
                profileName = profileName,
                backupDate = dateStr,
                appVersion = verStr
            )
        } catch (e: Exception) {
            BackupAnalysis(isValid = false, errorReason = "Failed to parse backup ZIP: ${e.message}")
        }
    }

    suspend fun restoreBackup(
        context: Context,
        uri: Uri,
        dao: TransactionDao,
        goalDao: SavingsGoalDao,
        database: AmarHishabDatabase
    ): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val stream = context.contentResolver.openInputStream(uri)
                ?: return@withContext RestoreResult(false, errorMessage = "Cannot open selected backup file")
            restoreBackupStream(context, stream, dao, goalDao, database)
        } catch (e: Exception) {
            RestoreResult(success = false, errorMessage = e.message ?: "Failed to restore backup")
        }
    }

    suspend fun restoreBackupStream(
        context: Context,
        inputStream: InputStream,
        dao: TransactionDao,
        goalDao: SavingsGoalDao,
        database: AmarHishabDatabase
    ): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val textFilesMap = mutableMapOf<String, String>()
            val binaryFilesMap = mutableMapOf<String, ByteArray>()

            ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val name = entry.name.lowercase().substringAfterLast('/')
                        val baos = ByteArrayOutputStream()
                        zis.copyTo(baos)
                        val bytes = baos.toByteArray()
                        if (name == "profile_photo.jpg" || name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg")) {
                            binaryFilesMap[name] = bytes
                        } else {
                            textFilesMap[name] = String(bytes, StandardCharsets.UTF_8)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            // Verify validity
            val hasTx = textFilesMap.containsKey("transactions.json") || textFilesMap.containsKey("amar_hishab_backup.json")
            if (!hasTx && !textFilesMap.containsKey("backup_metadata.json")) {
                return@withContext RestoreResult(false, errorMessage = "The selected file is not a valid Amar Hishab backup archive.")
            }

            val session = EncryptedSessionManager.getInstance(context)
            var restoredTx = 0
            var restoredPersonalTx = 0
            var restoredBusinessTx = 0
            var restoredCat = 0
            var restoredGoals = 0
            var profileRestored = false
            var settingsRestored = false
            var smartSpendingRestored = false

            // 1. Restore Profile & Profile Photo
            var restoredAvatarPath: String? = null
            val photoBytes = binaryFilesMap["profile_photo.jpg"]
                ?: binaryFilesMap.entries.firstOrNull { it.key.endsWith(".jpg") || it.key.endsWith(".png") }?.value

            if (photoBytes != null && photoBytes.isNotEmpty()) {
                try {
                    val avatarFile = File(context.filesDir, "user_avatar_${System.currentTimeMillis()}.jpg")
                    context.filesDir.listFiles { _, name -> name.startsWith("user_avatar") }?.forEach { it.delete() }
                    avatarFile.writeBytes(photoBytes)
                    restoredAvatarPath = avatarFile.absolutePath
                    session.updateProfileAvatar(restoredAvatarPath)
                    Log.d(TAG, "Restored profile photo to $restoredAvatarPath")
                } catch (e: Exception) {
                    Log.w(TAG, "Profile photo extraction note: ${e.message}")
                }
            }

            textFilesMap["profile.json"]?.let { profileJsonStr ->
                try {
                    val obj = JSONObject(profileJsonStr)
                    val name = obj.optString("name", "AJ SRABON").ifBlank { "AJ SRABON" }
                    val email = obj.optString("email", "ashrafuzzamansrabon@gmail.com")
                    val phone = obj.optString("phone", "+8801XXXXXXXXX")
                    session.saveUserProfile(name, email, phone, restoredAvatarPath)
                    profileRestored = true
                } catch (e: Exception) {
                    Log.w(TAG, "Profile JSON parse note: ${e.message}")
                }
            }

            // 2. Restore User Preferences & Settings
            textFilesMap["settings.json"]?.let { settingsJsonStr ->
                try {
                    val obj = JSONObject(settingsJsonStr)
                    if (obj.has("language")) session.setLanguage(obj.getString("language"))
                    if (obj.has("themeMode")) session.setTheme(obj.getString("themeMode"))
                    if (obj.has("activeWorkspace")) session.setWorkspace(Workspace.fromId(obj.getString("activeWorkspace")))
                    if (obj.has("isBalanceHidden")) session.setBalanceHidden(obj.getBoolean("isBalanceHidden"))
                    if (obj.has("notificationsEnabled")) session.setNotificationsEnabled(obj.getBoolean("notificationsEnabled"))
                    if (obj.has("expenseRemindersEnabled")) session.setExpenseReminders(obj.getBoolean("expenseRemindersEnabled"))
                    if (obj.has("monthlySummaryEnabled")) session.setMonthlySummaryEnabled(obj.getBoolean("monthlySummaryEnabled"))
                    if (obj.has("appLockEnabled")) session.setAppLockEnabled(obj.getBoolean("appLockEnabled"))
                    settingsRestored = true
                } catch (e: Exception) {
                    Log.w(TAG, "Settings restore note: ${e.message}")
                }
            }

            // 3. Restore Smart Spending Planner Settings
            textFilesMap["smart_spending.json"]?.let { smartJsonStr ->
                try {
                    val obj = JSONObject(smartJsonStr)
                    val planner = SpendingPlannerManager.getInstance(context)
                    if (obj.has("personalMonthlyLimit")) {
                        planner.setMonthlyLimit(Workspace.PERSONAL, obj.getDouble("personalMonthlyLimit"))
                    }
                    if (obj.has("businessMonthlyLimit")) {
                        planner.setMonthlyLimit(Workspace.BUSINESS, obj.getDouble("businessMonthlyLimit"))
                    }
                    if (obj.has("personalRecurringLimit")) {
                        planner.setRecurringLimitEnabled(Workspace.PERSONAL, obj.getBoolean("personalRecurringLimit"))
                    }
                    if (obj.has("businessRecurringLimit")) {
                        planner.setRecurringLimitEnabled(Workspace.BUSINESS, obj.getBoolean("businessRecurringLimit"))
                    }
                    smartSpendingRestored = true
                } catch (e: Exception) {
                    Log.w(TAG, "Smart spending restore note: ${e.message}")
                }
            }

            // 4. Restore Custom Categories (safe deduplication)
            textFilesMap["categories.json"]?.let { jsonStr ->
                val categoryManager = CategoryManager.getInstance(context)
                val catArray = JSONArray(jsonStr)
                for (i in 0 until catArray.length()) {
                    val obj = catArray.getJSONObject(i)
                    val id = obj.optString("id", "")
                    val nameEn = obj.optString("nameEn", "")
                    val nameBn = obj.optString("nameBn", nameEn)
                    val typeStr = obj.optString("type", "EXPENSE")
                    val type = if (typeStr.equals("INCOME", ignoreCase = true)) TransactionType.INCOME else TransactionType.EXPENSE
                    val iconName = obj.optString("iconName", "Category")
                    val colorHex = obj.optLong("colorHex", if (type == TransactionType.INCOME) 0xFF00897BL else 0xFFE65100L)
                    val workspace = obj.optString("workspace", "ALL")

                    val targetName = nameEn.ifBlank { nameBn }
                    if (targetName.isNotBlank() && !categoryManager.isDuplicateName(targetName, type)) {
                        categoryManager.addCategory(
                            name = targetName,
                            type = type,
                            colorHex = colorHex,
                            iconName = iconName,
                            workspace = workspace,
                            customId = id.takeIf { it.isNotBlank() },
                            nameBn = nameBn.takeIf { it.isNotBlank() }
                        )
                        restoredCat++
                    }
                }
            }

            // 5. Prepare Transactions (safe deduplication, stable IDs, Personal/Business preservation)
            val toInsertTx = mutableListOf<TransactionEntity>()
            val existingTx = dao.getAllTransactionsList()
            val existingIds = existingTx.map { it.id }.toSet()
            val existingKeys = existingTx.map {
                "${it.date.trim()}_${String.format(Locale.US, "%.2f", it.amount)}_${it.type.trim().uppercase()}_${it.category.trim().lowercase()}_${it.accountCode.trim().lowercase()}_${it.workspace.trim().uppercase()}"
            }.toSet()

            val txJsonStr = textFilesMap["transactions.json"]
                ?: textFilesMap["amar_hishab_backup.json"]?.let { legacy ->
                    try {
                        JSONObject(legacy).optJSONArray("transactions")?.toString()
                    } catch (_: Exception) { null }
                }

            if (txJsonStr != null) {
                val txArray = JSONArray(txJsonStr)
                for (i in 0 until txArray.length()) {
                    val obj = txArray.getJSONObject(i)
                    val id = obj.optString("id").takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
                    val date = obj.optString("date", SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
                    val amount = obj.getDouble("amount")
                    val typeStr = obj.optString("type", "EXPENSE")
                    val type = try {
                        TransactionType.valueOf(typeStr.uppercase()).value
                    } catch (_: Exception) {
                        TransactionType.fromString(typeStr).value
                    }
                    val category = obj.optString("category", "Other")
                    val accountCode = obj.optString("accountCode", obj.optString("account", "cash"))
                    val toAccountCode = obj.optString("toAccountCode").takeIf { it.isNotBlank() }
                    val desc = obj.optString("description", obj.optString("note", "")).takeIf { it.isNotBlank() }
                    val createdAt = obj.optLong("createdAt", obj.optLong("timestamp", System.currentTimeMillis()))
                    val ws = obj.optString("workspace", "PERSONAL").uppercase()
                    val normalizedWs = if (ws == "BUSINESS") "BUSINESS" else "PERSONAL"

                    val key = "${date.trim()}_${String.format(Locale.US, "%.2f", amount)}_${type.trim().uppercase()}_${category.trim().lowercase()}_${accountCode.trim().lowercase()}_$normalizedWs"

                    if (!existingIds.contains(id) && !existingKeys.contains(key)) {
                        toInsertTx.add(
                            TransactionEntity(
                                id = id,
                                type = type,
                                amount = amount,
                                category = category,
                                accountCode = accountCode,
                                toAccountCode = toAccountCode,
                                date = date,
                                description = desc,
                                createdAt = createdAt,
                                isSynced = false,
                                syncStatus = "SYNCED",
                                workspace = normalizedWs
                            )
                        )
                        if (normalizedWs == "BUSINESS") restoredBusinessTx++ else restoredPersonalTx++
                    }
                }
            }

            // 6. Prepare Savings Goals
            val toInsertGoals = mutableListOf<SavingsGoalEntity>()
            textFilesMap["goals.json"]?.let { goalJsonStr ->
                val existingGoals = goalDao.getAllGoalsList()
                val existingGoalIds = existingGoals.map { it.id }.toSet()
                val existingGoalNames = existingGoals.map { it.name.lowercase().trim() }.toSet()

                val goalArray = JSONArray(goalJsonStr)
                for (i in 0 until goalArray.length()) {
                    val obj = goalArray.getJSONObject(i)
                    val id = obj.optString("id").takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
                    val name = obj.getString("name")
                    val ws = obj.optString("workspace", "PERSONAL")

                    if (!existingGoalIds.contains(id) && !existingGoalNames.contains(name.lowercase().trim())) {
                        toInsertGoals.add(
                            SavingsGoalEntity(
                                id = id,
                                name = name,
                                targetAmount = obj.getDouble("targetAmount"),
                                savedAmount = obj.optDouble("savedAmount", 0.0),
                                targetDate = obj.getString("targetDate"),
                                description = obj.optString("description").takeIf { it.isNotBlank() },
                                iconCategory = obj.optString("iconCategory").takeIf { it.isNotBlank() },
                                isActiveOnDashboard = obj.optBoolean("isActiveOnDashboard", false),
                                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                                workspace = ws
                            )
                        )
                        restoredGoals++
                    }
                }
            }

            // Execute database transaction for non-destructive, safe commit
            database.withTransaction {
                if (toInsertTx.isNotEmpty()) {
                    dao.insertAll(toInsertTx)
                    restoredTx = toInsertTx.size
                }
                if (toInsertGoals.isNotEmpty()) {
                    goalDao.insertAll(toInsertGoals)
                }
            }

            RestoreResult(
                success = true,
                transactionsRestored = restoredTx,
                personalTransactionsRestored = restoredPersonalTx,
                businessTransactionsRestored = restoredBusinessTx,
                categoriesRestored = restoredCat,
                goalsRestored = restoredGoals,
                profileRestored = profileRestored,
                settingsRestored = settingsRestored,
                smartSpendingRestored = smartSpendingRestored,
                budgetsRestored = 0,
                recurringRestored = 0,
                remindersRestored = 0
            )
        } catch (e: Exception) {
            RestoreResult(
                success = false,
                errorMessage = e.message ?: "Failed to safely restore backup"
            )
        }
    }
}
