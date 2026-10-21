package com.ajyra.amarhishab.backup

import com.ajyra.amarhishab.model.Account
import com.ajyra.amarhishab.model.Transaction
import com.ajyra.amarhishab.model.TransactionType
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object BackupManager {

    const val BACKUP_ENTRY_NAME = "amar_hishab_backup.json"
    const val BACKUP_VERSION = 1

    fun createZipBackup(
        transactions: List<Transaction>,
        accounts: List<Account>,
        outputZipFile: File
    ): Boolean {
        return try {
            val rootJson = JSONObject().apply {
                put("version", BACKUP_VERSION)
                put("timestamp", System.currentTimeMillis())

                val txArray = JSONArray()
                transactions.forEach { tx ->
                    val obj = JSONObject().apply {
                        put("id", tx.id)
                        put("title", tx.title)
                        put("amount", tx.amount)
                        put("type", tx.type.name)
                        put("category", tx.category)
                        put("account", tx.account)
                        put("timestamp", tx.createdAt)
                        put("note", tx.note)
                        put("workspace", tx.workspace)
                    }
                    txArray.put(obj)
                }
                put("transactions", txArray)

                val accArray = JSONArray()
                accounts.forEach { acc ->
                    val obj = JSONObject().apply {
                        put("id", acc.id)
                        put("name", acc.name)
                        put("balance", acc.balance)
                    }
                    accArray.put(obj)
                }
                put("accounts", accArray)
            }

            val jsonBytes = rootJson.toString(2).toByteArray(StandardCharsets.UTF_8)

            ZipOutputStream(FileOutputStream(outputZipFile)).use { zos ->
                val entry = ZipEntry(BACKUP_ENTRY_NAME)
                zos.putNextEntry(entry)
                zos.write(jsonBytes)
                zos.closeEntry()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun readZipBackup(zipFile: File): Pair<List<Transaction>, List<Account>> {
        ZipFile(zipFile).use { zf ->
            val legacyEntry = zf.getEntry(BACKUP_ENTRY_NAME)
            val modernEntry = zf.getEntry("transactions.json")

            if (legacyEntry != null) {
                val content = zf.getInputStream(legacyEntry).use { stream ->
                    InputStreamReader(stream, StandardCharsets.UTF_8).readText()
                }
                val root = JSONObject(content)

                val txList = mutableListOf<Transaction>()
                val txArray = root.optJSONArray("transactions") ?: JSONArray()
                for (i in 0 until txArray.length()) {
                    val obj = txArray.getJSONObject(i)
                    val typeStr = obj.optString("type", "EXPENSE")
                    val type = try {
                        TransactionType.valueOf(typeStr.uppercase())
                    } catch (_: Exception) {
                        TransactionType.fromString(typeStr)
                    }
                    txList.add(
                        Transaction(
                            id = obj.getString("id"),
                            title = obj.optString("title", obj.optString("category", "Expense")),
                            amount = obj.getDouble("amount"),
                            type = type,
                            category = obj.optString("category", "Other"),
                            account = obj.optString("account", obj.optString("accountCode", "cash")),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            note = obj.optString("note", ""),
                            workspace = obj.optString("workspace", "PERSONAL")
                        )
                    )
                }

                val accList = mutableListOf<Account>()
                val accArray = root.optJSONArray("accounts") ?: JSONArray()
                for (i in 0 until accArray.length()) {
                    val obj = accArray.getJSONObject(i)
                    accList.add(
                        Account(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            balance = obj.optDouble("balance", 0.0)
                        )
                    )
                }

                return Pair(txList, accList)
            } else if (modernEntry != null) {
                val content = zf.getInputStream(modernEntry).use { stream ->
                    InputStreamReader(stream, StandardCharsets.UTF_8).readText()
                }
                val txArray = JSONArray(content)
                val txList = mutableListOf<Transaction>()
                for (i in 0 until txArray.length()) {
                    val obj = txArray.getJSONObject(i)
                    val typeStr = obj.optString("type", "EXPENSE")
                    val type = try {
                        TransactionType.valueOf(typeStr.uppercase())
                    } catch (_: Exception) {
                        TransactionType.fromString(typeStr)
                    }
                    txList.add(
                        Transaction(
                            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                            title = obj.optString("description", obj.optString("category", "Expense")),
                            amount = obj.getDouble("amount"),
                            type = type,
                            category = obj.optString("category", "Other"),
                            account = obj.optString("accountCode", "cash"),
                            timestamp = obj.optLong("createdAt", System.currentTimeMillis()),
                            note = obj.optString("description", ""),
                            workspace = obj.optString("workspace", "PERSONAL")
                        )
                    )
                }
                return Pair(txList, emptyList())
            } else {
                throw IllegalArgumentException("Not a valid Amar Hishab ZIP backup")
            }
        }
    }

    fun restoreWithDeduplication(
        existing: List<Transaction>,
        incoming: List<Transaction>
    ): List<Transaction> {
        val existingIds = existing.map { it.id }.toSet()
        val newTx = incoming.filter { it.id !in existingIds }
        return existing + newTx
    }
}
