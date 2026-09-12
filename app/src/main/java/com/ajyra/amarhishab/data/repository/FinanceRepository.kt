package com.ajyra.amarhishab.data.repository

import android.util.Log
import com.ajyra.amarhishab.data.local.TransactionDao
import com.ajyra.amarhishab.data.local.TransactionEntity
import com.ajyra.amarhishab.model.AccountExpense
import com.ajyra.amarhishab.model.AccountType
import com.ajyra.amarhishab.model.AppUpdateInfo
import com.ajyra.amarhishab.model.CategoryExpense
import com.ajyra.amarhishab.model.FinancialSummary
import com.ajyra.amarhishab.model.MonthlyReport
import com.ajyra.amarhishab.model.SyncStatus
import com.ajyra.amarhishab.model.Transaction
import com.ajyra.amarhishab.model.TransactionType
import com.ajyra.amarhishab.network.AmarHishabApiService
import com.ajyra.amarhishab.network.CreateTransactionRequestDto
import com.ajyra.amarhishab.network.NetworkErrorParser
import com.ajyra.amarhishab.network.NetworkResult
import com.ajyra.amarhishab.network.TransferRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class FinanceRepository(
    private val transactionDao: TransactionDao,
    private val apiService: AmarHishabApiService
) {
    fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getRecentTransactions(limit: Int = 10): Flow<List<Transaction>> {
        return transactionDao.getRecentTransactions(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getFinancialSummary(): Flow<FinancialSummary> {
        return getAllTransactions().map { transactions ->
            computeFinancialSummary(transactions)
        }
    }

    private fun computeFinancialSummary(transactions: List<Transaction>): FinancialSummary {
        var cash = 0.0
        var bkash = 0.0
        var nagad = 0.0
        var bank = 0.0
        var totalIncome = 0.0
        var totalExpense = 0.0

        for (tx in transactions) {
            when (tx.type) {
                TransactionType.INCOME -> {
                    totalIncome += tx.amount
                    when (AccountType.fromCode(tx.accountCode)) {
                        AccountType.CASH -> cash += tx.amount
                        AccountType.BKASH -> bkash += tx.amount
                        AccountType.NAGAD -> nagad += tx.amount
                        AccountType.BANK -> bank += tx.amount
                    }
                }
                TransactionType.EXPENSE -> {
                    totalExpense += tx.amount
                    when (AccountType.fromCode(tx.accountCode)) {
                        AccountType.CASH -> cash -= tx.amount
                        AccountType.BKASH -> bkash -= tx.amount
                        AccountType.NAGAD -> nagad -= tx.amount
                        AccountType.BANK -> bank -= tx.amount
                    }
                }
                TransactionType.TRANSFER -> {
                    when (AccountType.fromCode(tx.accountCode)) {
                        AccountType.CASH -> cash -= tx.amount
                        AccountType.BKASH -> bkash -= tx.amount
                        AccountType.NAGAD -> nagad -= tx.amount
                        AccountType.BANK -> bank -= tx.amount
                    }
                    if (tx.toAccountCode != null) {
                        when (AccountType.fromCode(tx.toAccountCode)) {
                            AccountType.CASH -> cash += tx.amount
                            AccountType.BKASH -> bkash += tx.amount
                            AccountType.NAGAD -> nagad += tx.amount
                            AccountType.BANK -> bank += tx.amount
                        }
                    }
                }
            }
        }

        val totalBalance = cash + bkash + nagad + bank
        val totalSaved = totalIncome - totalExpense
        val savingsRate = if (totalIncome > 0.0) (totalSaved / totalIncome) * 100.0 else 0.0

        return FinancialSummary(
            totalBalance = totalBalance,
            cashBalance = cash,
            bkashBalance = bkash,
            nagadBalance = nagad,
            bankBalance = bank,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            totalSaved = totalSaved,
            savingsRate = savingsRate
        )
    }

    suspend fun getMonthlyReport(year: Int, month: Int): MonthlyReport = withContext(Dispatchers.IO) {
        val all = getAllTransactions().firstOrNull().orEmpty()
        val monthStr = String.format("%02d", month)
        val prefix = "$year-$monthStr"

        val monthTxs = all.filter { it.date.startsWith(prefix) }
        var income = 0.0
        var expense = 0.0
        val categoryMap = mutableMapOf<String, Double>()
        val accountMap = mutableMapOf<String, Double>()

        for (tx in monthTxs) {
            when (tx.type) {
                TransactionType.INCOME -> income += tx.amount
                TransactionType.EXPENSE -> {
                    expense += tx.amount
                    categoryMap[tx.category] = (categoryMap[tx.category] ?: 0.0) + tx.amount
                    accountMap[tx.accountCode] = (accountMap[tx.accountCode] ?: 0.0) + tx.amount
                }
                TransactionType.TRANSFER -> {
                    // Not included in income/expense report
                }
            }
        }

        val saved = income - expense

        val categoryBreakdown = categoryMap.map { (cat, amt) ->
            val pct = if (expense > 0.0) (amt / expense) * 100.0 else 0.0
            CategoryExpense(category = cat, amount = amt, percentage = pct)
        }.sortedByDescending { it.amount }

        val accountBreakdown = accountMap.map { (code, amt) ->
            val pct = if (expense > 0.0) (amt / expense) * 100.0 else 0.0
            AccountExpense(accountCode = code, amount = amt, percentage = pct)
        }.sortedByDescending { it.amount }

        MonthlyReport(
            month = month,
            year = year,
            income = income,
            expense = expense,
            saved = saved,
            categoryBreakdown = categoryBreakdown,
            paymentMethodBreakdown = accountBreakdown
        )
    }

    suspend fun addTransaction(
        type: TransactionType,
        amount: Double,
        category: String,
        accountCode: String,
        toAccountCode: String? = null,
        date: String,
        description: String? = null
    ): NetworkResult<Transaction> = withContext(Dispatchers.IO) {
        val localId = UUID.randomUUID().toString()
        val localTx = Transaction(
            id = localId,
            type = type,
            amount = amount,
            category = category,
            accountCode = accountCode,
            toAccountCode = toAccountCode,
            date = date,
            description = description,
            createdAt = System.currentTimeMillis(),
            isSynced = false,
            syncStatus = SyncStatus.PENDING_CREATE
        )
        // Save immediately locally for zero-loss offline support
        transactionDao.insert(TransactionEntity.fromDomain(localTx))

        try {
            val req = CreateTransactionRequestDto(
                type = type.value,
                amount = amount,
                category = category,
                account = accountCode,
                toAccount = toAccountCode,
                date = date,
                description = description
            )
            val remoteDto = apiService.createTransaction(req)
            val syncedTx = localTx.copy(
                id = remoteDto.id ?: localId,
                isSynced = true,
                syncStatus = SyncStatus.SYNCED
            )
            if (remoteDto.id != null && remoteDto.id != localId) {
                transactionDao.deleteById(localId)
            }
            transactionDao.insert(TransactionEntity.fromDomain(syncedTx))
            NetworkResult.Success(syncedTx)
        } catch (e: Exception) {
            Log.w(TAG, "Online sync failed, preserved locally: ${e.message}")
            NetworkResult.Success(localTx)
        }
    }

    suspend fun updateTransaction(
        id: String,
        type: TransactionType,
        amount: Double,
        category: String,
        accountCode: String,
        toAccountCode: String? = null,
        date: String,
        description: String? = null
    ): NetworkResult<Transaction> = withContext(Dispatchers.IO) {
        val updatedTx = Transaction(
            id = id,
            type = type,
            amount = amount,
            category = category,
            accountCode = accountCode,
            toAccountCode = toAccountCode,
            date = date,
            description = description,
            isSynced = false,
            syncStatus = SyncStatus.PENDING_UPDATE
        )
        transactionDao.update(TransactionEntity.fromDomain(updatedTx))

        try {
            val req = CreateTransactionRequestDto(
                type = type.value,
                amount = amount,
                category = category,
                account = accountCode,
                toAccount = toAccountCode,
                date = date,
                description = description
            )
            val remoteDto = apiService.updateTransaction(id, req)
            val syncedTx = updatedTx.copy(isSynced = true, syncStatus = SyncStatus.SYNCED)
            transactionDao.update(TransactionEntity.fromDomain(syncedTx))
            NetworkResult.Success(syncedTx)
        } catch (e: Exception) {
            Log.w(TAG, "Update sync failed, preserved locally: ${e.message}")
            NetworkResult.Success(updatedTx)
        }
    }

    suspend fun deleteTransaction(id: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        transactionDao.deleteById(id)
        try {
            apiService.deleteTransaction(id)
        } catch (e: Exception) {
            Log.w(TAG, "Delete remote call failed: ${e.message}")
        }
        NetworkResult.Success(Unit)
    }

    suspend fun transferMoney(
        fromAccount: String,
        toAccount: String,
        amount: Double,
        date: String,
        description: String?
    ): NetworkResult<Transaction> = withContext(Dispatchers.IO) {
        val localId = UUID.randomUUID().toString()
        val localTx = Transaction(
            id = localId,
            type = TransactionType.TRANSFER,
            amount = amount,
            category = "transfer",
            accountCode = fromAccount,
            toAccountCode = toAccount,
            date = date,
            description = description ?: "Transfer from $fromAccount to $toAccount",
            createdAt = System.currentTimeMillis(),
            isSynced = false,
            syncStatus = SyncStatus.PENDING_CREATE
        )
        transactionDao.insert(TransactionEntity.fromDomain(localTx))

        try {
            val req = TransferRequestDto(
                fromAccount = fromAccount,
                toAccount = toAccount,
                amount = amount,
                date = date,
                description = description
            )
            val remoteDto = apiService.transferMoney(req)
            val syncedTx = localTx.copy(
                id = remoteDto.id ?: localId,
                isSynced = true,
                syncStatus = SyncStatus.SYNCED
            )
            if (remoteDto.id != null && remoteDto.id != localId) {
                transactionDao.deleteById(localId)
            }
            transactionDao.insert(TransactionEntity.fromDomain(syncedTx))
            NetworkResult.Success(syncedTx)
        } catch (e: Exception) {
            Log.w(TAG, "Transfer remote sync failed, preserved locally: ${e.message}")
            NetworkResult.Success(localTx)
        }
    }

    suspend fun syncWithBackend(): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        try {
            // Fetch remote transactions
            val remoteList = apiService.getTransactions()
            val entities = remoteList.map { dto ->
                TransactionEntity(
                    id = dto.id ?: UUID.randomUUID().toString(),
                    type = dto.type ?: "expense",
                    amount = dto.amount ?: 0.0,
                    category = dto.category ?: "other_expense",
                    accountCode = dto.account ?: "cash",
                    toAccountCode = dto.toAccount,
                    date = dto.date ?: "",
                    description = dto.description,
                    createdAt = System.currentTimeMillis(),
                    isSynced = true,
                    syncStatus = "SYNCED"
                )
            }
            if (entities.isNotEmpty()) {
                transactionDao.insertAll(entities)
            }
            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Backend sync deferred: ${e.message}")
            NetworkErrorParser.parse(e)
        }
    }

    suspend fun checkAppUpdate(): NetworkResult<AppUpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val dto = apiService.checkAppVersion()
            NetworkResult.Success(
                AppUpdateInfo(
                    latestVersion = dto.latestVersion ?: "1.0.0",
                    latestVersionCode = dto.latestVersionCode ?: 1,
                    minSupportedVersion = dto.minSupportedVersion ?: "1.0.0",
                    updateRequired = dto.updateRequired ?: false,
                    releaseNotes = dto.releaseNotes ?: "",
                    downloadUrl = dto.downloadUrl ?: ""
                )
            )
        } catch (e: Exception) {
            NetworkErrorParser.parse(e)
        }
    }

    suspend fun clearLocalData() = withContext(Dispatchers.IO) {
        transactionDao.deleteAll()
    }

    companion object {
        private const val TAG = "FinanceRepository"
    }
}
