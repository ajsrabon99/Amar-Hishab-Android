package com.ajyra.amarhishab

import com.ajyra.amarhishab.backup.BackupManager
import com.ajyra.amarhishab.model.Account
import com.ajyra.amarhishab.model.Transaction
import com.ajyra.amarhishab.model.TransactionType
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupRestoreAndEditTransactionTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testZipBackupCreationAndContentVerification() {
        val backupFile = File(tempFolder.root, "test_backup.zip")
        val transactions = listOf(
            Transaction(
                id = "tx-101",
                title = "Office Salary",
                amount = 75000.0,
                type = TransactionType.INCOME,
                category = "Salary",
                account = "City Bank",
                note = "Monthly payment"
            ),
            Transaction(
                id = "tx-102",
                title = "Grocery Bazar",
                amount = 6200.0,
                type = TransactionType.EXPENSE,
                category = "Food",
                account = "Cash"
            )
        )
        val accounts = listOf(
            Account("acc-1", "City Bank", 100000.0),
            Account("acc-2", "Cash", 15000.0)
        )

        val created = BackupManager.createZipBackup(transactions, accounts, backupFile)
        assertTrue("ZIP backup creation should succeed", created)
        assertTrue("Backup file must exist", backupFile.exists())
        assertTrue("Backup file size must be > 0", backupFile.length() > 0)

        val (restoredTx, restoredAcc) = BackupManager.readZipBackup(backupFile)
        assertEquals(2, restoredTx.size)
        assertEquals(2, restoredAcc.size)

        val firstTx = restoredTx.first { it.id == "tx-101" }
        assertEquals("Office Salary", firstTx.title)
        assertEquals(75000.0, firstTx.amount, 0.001)
        assertEquals(TransactionType.INCOME, firstTx.type)
        assertEquals("City Bank", firstTx.account)
        assertEquals("Monthly payment", firstTx.note)
    }

    @Test
    fun testRestoreDeduplicationAndDataIntegrity() {
        val existingTx = listOf(
            Transaction("tx-1", "Snacks", 150.0, TransactionType.EXPENSE, "Food", "Cash"),
            Transaction("tx-2", "Bus Fare", 50.0, TransactionType.EXPENSE, "Transport", "Cash")
        )

        val incomingTx = listOf(
            Transaction("tx-2", "Bus Fare", 50.0, TransactionType.EXPENSE, "Transport", "Cash"), // duplicate ID
            Transaction("tx-3", "Freelance Gig", 12000.0, TransactionType.INCOME, "Freelance", "bKash")
        )

        val merged = BackupManager.restoreWithDeduplication(existingTx, incomingTx)

        assertEquals("Deduplication must keep unique items only", 3, merged.size)
        assertTrue(merged.any { it.id == "tx-1" })
        assertTrue(merged.any { it.id == "tx-2" })
        assertTrue(merged.any { it.id == "tx-3" })

        val freelance = merged.first { it.id == "tx-3" }
        assertEquals(12000.0, freelance.amount, 0.001)
        assertEquals(TransactionType.INCOME, freelance.type)
    }

    @Test
    fun testEditTransactionUpdatesValues() {
        var tx = Transaction("tx-4", "Electric Bill", 1800.0, TransactionType.EXPENSE, "Bills", "bKash")
        tx = tx.copy(amount = 2100.0, note = "Late fine included")

        assertEquals(2100.0, tx.amount, 0.001)
        assertEquals("Late fine included", tx.note)
    }

    @Test
    fun testEmptyBackupFileHandling() {
        val emptyList = emptyList<Transaction>()
        val emptyAcc = emptyList<Account>()
        val backupFile = File(tempFolder.root, "empty_backup.zip")

        val created = BackupManager.createZipBackup(emptyList, emptyAcc, backupFile)
        assertTrue(created)

        val (tx, acc) = BackupManager.readZipBackup(backupFile)
        assertTrue(tx.isEmpty())
        assertTrue(acc.isEmpty())
    }

    @Test
    fun testMultipleAccountsBackupAndRestore() {
        val backupFile = File(tempFolder.root, "multi_acc_backup.zip")
        val accounts = listOf(
            Account("acc-1", "Brac Bank", 25000.0),
            Account("acc-2", "Nagad", 5000.0),
            Account("acc-3", "Rocket", 2000.0)
        )

        BackupManager.createZipBackup(emptyList(), accounts, backupFile)
        val (_, restoredAcc) = BackupManager.readZipBackup(backupFile)

        assertEquals(3, restoredAcc.size)
        val nagad = restoredAcc.first { it.id == "acc-2" }
        assertEquals(5000.0, nagad.balance, 0.001)
    }

    @Test
    fun testIncomeAndExpenseBalanceCalculation() {
        val transactions = listOf(
            Transaction("1", "Salary", 50000.0, TransactionType.INCOME, "Job", "Bank"),
            Transaction("2", "Rent", 15000.0, TransactionType.EXPENSE, "Housing", "Bank"),
            Transaction("3", "Groceries", 7000.0, TransactionType.EXPENSE, "Food", "Cash")
        )

        val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val netBalance = income - expense

        assertEquals(50000.0, income, 0.001)
        assertEquals(22000.0, expense, 0.001)
        assertEquals(28000.0, netBalance, 0.001)
    }

    @Test
    fun testCompleteBackupArchiveAnalysisWithProfilePhotoAndSettings() {
        val testZip = File(tempFolder.root, "complete_state_backup.zip")
        java.util.zip.ZipOutputStream(java.io.FileOutputStream(testZip)).use { zos ->
            fun addEntry(name: String, content: ByteArray) {
                zos.putNextEntry(java.util.zip.ZipEntry(name))
                zos.write(content)
                zos.closeEntry()
            }

            val metaJson = org.json.JSONObject().apply {
                put("appName", "Amar Hishab")
                put("appVersion", "1.3.0")
                put("transactionCount", 4)
                put("personalTransactionCount", 3)
                put("businessTransactionCount", 1)
                put("categoryCount", 2)
                put("goalCount", 1)
                put("hasProfile", true)
                put("hasProfilePhoto", true)
                put("profileName", "AJ SRABON")
                put("hasSettings", true)
                put("hasSmartSpending", true)
                put("backupDateFormatted", "22 October 2026, 12:00")
            }
            addEntry("backup_metadata.json", metaJson.toString().toByteArray(Charsets.UTF_8))

            val txJson = org.json.JSONArray().apply {
                put(org.json.JSONObject().apply {
                    put("id", "tx-p1")
                    put("type", "INCOME")
                    put("amount", 50000.0)
                    put("workspace", "PERSONAL")
                })
                put(org.json.JSONObject().apply {
                    put("id", "tx-b1")
                    put("type", "EXPENSE")
                    put("amount", 12000.0)
                    put("workspace", "BUSINESS")
                })
            }
            addEntry("transactions.json", txJson.toString().toByteArray(Charsets.UTF_8))

            val profileJson = org.json.JSONObject().apply {
                put("name", "AJ SRABON")
                put("phone", "+8801327206804")
                put("hasPhoto", true)
            }
            addEntry("profile.json", profileJson.toString().toByteArray(Charsets.UTF_8))

            val settingsJson = org.json.JSONObject().apply {
                put("language", "bn")
                put("themeMode", "dark")
                put("isBalanceHidden", false)
            }
            addEntry("settings.json", settingsJson.toString().toByteArray(Charsets.UTF_8))

            // Fake binary image bytes
            val fakePhotoBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
            addEntry("profile_photo.jpg", fakePhotoBytes)
        }

        val analysis = java.io.FileInputStream(testZip).use { stream ->
            com.ajyra.amarhishab.data.service.DataBackupService.analyzeBackupStream(stream)
        }

        assertTrue("Analysis must be valid", analysis.isValid)
        assertTrue("Analysis must detect profile data", analysis.hasProfileData)
        assertTrue("Analysis must detect binary profile photo", analysis.hasProfilePhoto)
        assertTrue("Analysis must detect settings", analysis.hasSettings)
        assertTrue("Analysis must detect smart spending", analysis.hasSmartSpending)
        assertEquals("AJ SRABON", analysis.profileName)
        assertEquals("1.3.0", analysis.appVersion)
    }
}
