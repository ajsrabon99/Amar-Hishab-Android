package com.ajyra.amarhishab.data.repository

import android.util.Log
import com.ajyra.amarhishab.data.local.SavingsGoalDao
import com.ajyra.amarhishab.data.local.SavingsGoalEntity
import com.ajyra.amarhishab.data.local.TransactionDao
import com.ajyra.amarhishab.data.local.TransactionEntity
import com.ajyra.amarhishab.model.AccountExpense
import com.ajyra.amarhishab.model.AccountType
import com.ajyra.amarhishab.model.AppUpdateInfo
import com.ajyra.amarhishab.model.CategoryExpense
import com.ajyra.amarhishab.model.FinancialSummary
import com.ajyra.amarhishab.model.MonthlyReport
import com.ajyra.amarhishab.model.SavingsGoal
import com.ajyra.amarhishab.model.SmartSavingSuggestion
import com.ajyra.amarhishab.model.SmartSavingsPlan
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.max
import kotlin.math.round

class FinanceRepository(
    private val transactionDao: TransactionDao,
    private val apiService: AmarHishabApiService,
    private val savingsGoalDao: SavingsGoalDao? = null
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

    fun getCategoryExpenses(): Flow<List<CategoryExpense>> {
        return getAllTransactions().map { transactions ->
            computeCategoryExpenses(transactions)
        }
    }

    private fun computeCategoryExpenses(transactions: List<Transaction>): List<CategoryExpense> {
        val expenseTxs = transactions.filter { it.type == TransactionType.EXPENSE }
        val totalExpense = expenseTxs.sumOf { it.amount }
        if (totalExpense <= 0.0) return emptyList()

        val categoryMap = mutableMapOf<String, Double>()
        for (tx in expenseTxs) {
            val cat = tx.category.ifBlank { "Other Expense" }
            categoryMap[cat] = (categoryMap[cat] ?: 0.0) + tx.amount
        }

        return categoryMap.map { (cat, amt) ->
            val pct = (amt / totalExpense) * 100.0
            CategoryExpense(category = cat, amount = amt, percentage = pct)
        }.sortedByDescending { it.amount }
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

    // =========================================================================
    // SAVINGS GOALS & SMART SUGGESTIONS
    // =========================================================================

    fun getAllSavingsGoals(): Flow<List<SavingsGoal>> {
        val dao = savingsGoalDao ?: return flowOf(emptyList())
        return dao.getAllGoals().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getActiveSavingsGoal(): Flow<SavingsGoal?> {
        val dao = savingsGoalDao ?: return flowOf(null)
        return dao.getAllGoals().map { entities ->
            // Priority: explicitly active goal on dashboard, or first in-progress goal, or first goal
            val active = entities.firstOrNull { it.isActiveOnDashboard }
                ?: entities.firstOrNull { it.savedAmount < it.targetAmount }
                ?: entities.firstOrNull()
            active?.toDomain()
        }
    }

    suspend fun createSavingsGoal(
        name: String,
        targetAmount: Double,
        targetDate: String,
        description: String? = null,
        iconCategory: String? = "general"
    ): SavingsGoal = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val dao = savingsGoalDao ?: throw IllegalStateException("SavingsGoalDao not available")
        val isFirst = dao.getCount() == 0
        val entity = SavingsGoalEntity(
            id = id,
            name = name.trim(),
            targetAmount = targetAmount,
            savedAmount = 0.0,
            targetDate = targetDate,
            description = description?.trim(),
            iconCategory = iconCategory ?: "general",
            isActiveOnDashboard = isFirst,
            createdAt = System.currentTimeMillis()
        )
        dao.insert(entity)
        entity.toDomain()
    }

    suspend fun updateSavingsGoal(goal: SavingsGoal) = withContext(Dispatchers.IO) {
        val dao = savingsGoalDao ?: return@withContext
        dao.update(SavingsGoalEntity.fromDomain(goal))
    }

    suspend fun deleteSavingsGoal(id: String) = withContext(Dispatchers.IO) {
        val dao = savingsGoalDao ?: return@withContext
        dao.deleteById(id)
    }

    suspend fun addMoneyToGoal(id: String, amount: Double) = withContext(Dispatchers.IO) {
        if (amount <= 0.0) return@withContext
        val dao = savingsGoalDao ?: return@withContext
        dao.addMoneyToGoal(id, amount)
    }

    suspend fun setActiveDashboardGoal(id: String) = withContext(Dispatchers.IO) {
        val dao = savingsGoalDao ?: return@withContext
        dao.setActiveDashboardGoal(id)
    }

    /**
     * Analyzes actual transaction data to generate practical smart savings suggestions.
     * Categorizes essential (Rent, Bills, WiFi, Utilities, Medical) vs discretionary (Food, Shopping, Entertainment, etc.)
     * and suggests practical, realistic reductions based on what the user actually spent.
     */
    fun getSmartSavingsPlan(goal: SavingsGoal): Flow<SmartSavingsPlan> {
        return getAllTransactions().map { transactions ->
            computeSmartSavingsPlan(goal, transactions)
        }
    }

    private fun computeSmartSavingsPlan(goal: SavingsGoal, transactions: List<Transaction>): SmartSavingsPlan {
        val daysRemaining = goal.getDaysRemaining()
        val requiredMonthly = goal.getRequiredMonthly(daysRemaining)

        val expenseTxs = transactions.filter { it.type == TransactionType.EXPENSE }
        if (expenseTxs.isEmpty() || goal.isCompleted || requiredMonthly <= 0.0) {
            return SmartSavingsPlan(
                goalId = goal.id,
                goalName = goal.name,
                requiredMonthly = requiredMonthly,
                suggestions = emptyList(),
                totalPotentialMonthlySavings = 0.0,
                remainingRequiredMonthly = requiredMonthly
            )
        }

        // Sum up total spending by category
        val categoryExpenses = mutableMapOf<String, Double>()
        for (tx in expenseTxs) {
            val cat = tx.category.trim()
            if (cat.isNotBlank()) {
                categoryExpenses[cat] = (categoryExpenses[cat] ?: 0.0) + tx.amount
            }
        }

        // Essential categories that should NOT be suggested for reduction
        val essentialKeywords = setOf(
            "rent", "house_rent", "house rent", "বাড়ি ভাড়া", "ভাড়া",
            "electricity", "বিদ্যুৎ", "বিদ্যুৎ বিল",
            "gas", "গ্যাস", "গ্যাস বিল",
            "wifi", "internet", "ইন্টারনেট", "ওয়াইফাই",
            "bill", "bills", "utility", "utilities", "বিল", "ইউটিলিটি",
            "health", "medical", "medicine", "চিকিৎসা", "ঔষধ",
            "education", "tuition", "শিক্ষা", "স্কুল", "টিউশন"
        )

        fun isEssentialCategory(name: String): Boolean {
            val lower = name.lowercase().trim()
            return essentialKeywords.any { keyword -> lower.contains(keyword) }
        }

        val suggestions = mutableListOf<SmartSavingSuggestion>()

        // Analyze discretionary categories
        categoryExpenses.forEach { (categoryName, totalSpent) ->
            val isEssential = isEssentialCategory(categoryName)
            if (!isEssential && totalSpent >= 200.0) {
                // Determine sensible reduction percentage based on category
                val lower = categoryName.lowercase()
                val (reductionPct, enReason, bnReason) = when {
                    lower.contains("shopping") || lower.contains("কেনাকাটা") || lower.contains("শপিং") -> {
                        Triple(
                            15,
                            "Reducing discretionary shopping by 15% can accelerate your '${goal.name}' goal.",
                            "শপিং বা কেনাকাটায় ১৫% ব্যয় হ্রাস করলে '${goal.name}' লক্ষ্য দ্রুত অর্জন সম্ভব।"
                        )
                    }
                    lower.contains("entertainment") || lower.contains("বিনোদন") || lower.contains("movie") || lower.contains("cinema") -> {
                        Triple(
                            20,
                            "Trimming entertainment spending by 20% frees up significant savings.",
                            "বিনোদন খরচে ২০% সাশ্রয় করলে উল্লেখযোগ্য পরিমাণ টাকা সঞ্চয় করা যাবে।"
                        )
                    }
                    lower.contains("food") || lower.contains("dining") || lower.contains("restaurant") || lower.contains("খাবার") -> {
                        Triple(
                            10,
                            "Moderating dining out by 10% can contribute directly to your goal.",
                            "বাইরে খাওয়ার খরচ ১০% কমালে প্রতি মাসে একটি ভালো অঙ্কের অর্থ সঞ্চয় হবে।"
                        )
                    }
                    lower.contains("snack") || lower.contains("নাস্তা") || lower.contains("চা") || lower.contains("tea") -> {
                        Triple(
                            15,
                            "Cutting minor snacks and coffee runs by 15% adds up over time.",
                            "দৈনন্দিন চা-নাস্তার খরচ সামান্য ১৫% কমালেই বড় সাশ্রয় সম্ভব।"
                        )
                    }
                    lower.contains("electronics") || lower.contains("gadget") || lower.contains("গ্যাজেট") -> {
                        Triple(
                            15,
                            "Delaying non-urgent gadget purchases creates immediate savings headroom.",
                            "অনাবশ্যক গ্যাজেট কেনা পিছিয়ে দিলে তাৎক্ষণিক সঞ্চয় তহবিল গড়ে তোলা যায়।"
                        )
                    }
                    else -> {
                        Triple(
                            10,
                            "Saving 10% on $categoryName expenses helps build your target fund.",
                            "$categoryName খাতে ১০% ব্যয় সংকোচন আপনার সঞ্চয় তহবিলে গতি আনবে।"
                        )
                    }
                }

                val potentialSaving = round((totalSpent * (reductionPct / 100.0)) / 10.0) * 10.0
                if (potentialSaving >= 50.0) {
                    suggestions.add(
                        SmartSavingSuggestion(
                            category = categoryName,
                            currentMonthlyExpense = totalSpent,
                            suggestedReductionPercentage = reductionPct,
                            potentialMonthlySavings = potentialSaving,
                            reasonEn = enReason,
                            reasonBn = bnReason,
                            isEssential = false
                        )
                    )
                }
            }
        }

        // Sort suggestions by highest potential savings
        suggestions.sortByDescending { it.potentialMonthlySavings }
        val topSuggestions = suggestions.take(4)
        val totalPotentialSavings = topSuggestions.sumOf { it.potentialMonthlySavings }
        val remainingRequired = max(0.0, requiredMonthly - totalPotentialSavings)

        return SmartSavingsPlan(
            goalId = goal.id,
            goalName = goal.name,
            requiredMonthly = requiredMonthly,
            suggestions = topSuggestions,
            totalPotentialMonthlySavings = totalPotentialSavings,
            remainingRequiredMonthly = remainingRequired
        )
    }

    companion object {
        private const val TAG = "FinanceRepository"
    }
}

