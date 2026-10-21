package com.ajyra.amarhishab.model

enum class TransactionType(val value: String) {
    INCOME("income"),
    EXPENSE("expense"),
    TRANSFER("transfer");

    companion object {
        fun fromString(value: String): TransactionType {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: EXPENSE
        }
    }
}

enum class AccountType(
    val code: String,
    val nameEn: String,
    val nameBn: String,
    val colorHex: Long
) {
    CASH("cash", "Cash", "ক্যাশ", 0xFF00897BL),
    BKASH("bkash", "bKash", "বিকাশ", 0xFFD81B60L),
    NAGAD("nagad", "Nagad", "নগদ", 0xFFED1C24L),
    BANK("bank", "Bank Account", "ব্যাংক অ্যাকাউন্ট", 0xFF1E88E5L);

    val displayNameEn: String get() = nameEn
    val displayNameBn: String get() = nameBn

    companion object {
        fun fromCode(code: String): AccountType {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: CASH
        }
    }
}

enum class Workspace(val id: String, val titleEn: String, val titleBn: String) {
    PERSONAL("PERSONAL", "Personal Account", "ব্যক্তিগত হিসাব"),
    BUSINESS("BUSINESS", "Business Account", "ব্যবসায়িক হিসাব");

    companion object {
        fun fromId(id: String?): Workspace {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: PERSONAL
        }
    }
}

enum class SyncStatus {
    SYNCED,
    PENDING_CREATE,
    PENDING_UPDATE,
    PENDING_DELETE,
    FAILED
}

data class Transaction(
    val id: String,
    val type: TransactionType,
    val amount: Double,
    val category: String,
    val accountCode: String,
    val toAccountCode: String? = null,
    val date: String = "",
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = true,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val note: String = "",
    val workspace: String = "PERSONAL"
) {
    constructor(
        id: String,
        title: String,
        amount: Double,
        type: TransactionType,
        category: String,
        account: String,
        timestamp: Long = System.currentTimeMillis(),
        note: String = "",
        workspace: String = "PERSONAL"
    ) : this(
        id = id,
        type = type,
        amount = amount,
        category = category,
        accountCode = account,
        toAccountCode = null,
        date = "",
        description = if (note.isNotBlank()) "$title: $note" else title,
        createdAt = timestamp,
        isSynced = true,
        syncStatus = SyncStatus.SYNCED,
        note = note,
        workspace = workspace
    )

    val title: String get() = description?.substringBefore(":") ?: category
    val account: String get() = accountCode
}

data class CategoryItem(
    val id: String,
    val nameEn: String,
    val nameBn: String,
    val type: TransactionType,
    val iconName: String,
    val colorHex: Long,
    val workspace: String = "ALL"
)

data class AccountExpense(
    val accountCode: String,
    val amount: Double,
    val percentage: Double
)

data class CategoryExpense(
    val category: String,
    val amount: Double,
    val percentage: Double
)

data class FinancialSummary(
    val totalBalance: Double = 0.0,
    val cashBalance: Double = 0.0,
    val bkashBalance: Double = 0.0,
    val nagadBalance: Double = 0.0,
    val bankBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalSaved: Double = 0.0,
    val savingsRate: Double = 0.0
)

data class MonthlyReport(
    val month: Int,
    val year: Int,
    val income: Double,
    val expense: Double,
    val saved: Double,
    val categoryBreakdown: List<CategoryExpense>,
    val paymentMethodBreakdown: List<AccountExpense>,
    val transactionCount: Int = 0,
    val incomeBreakdown: List<CategoryExpense> = emptyList(),
    val periodLabel: String = ""
) {
    val savingsRate: Double
        get() = if (income > 0.0) (saved / income) * 100.0 else 0.0
}

data class MonthlyComparison(
    val thisMonthIncome: Double = 0.0,
    val prevMonthIncome: Double = 0.0,
    val incomeChangePct: Double = 0.0,
    val thisMonthExpense: Double = 0.0,
    val prevMonthExpense: Double = 0.0,
    val expenseChangePct: Double = 0.0,
    val thisMonthNet: Double = 0.0,
    val prevMonthNet: Double = 0.0,
    val netChangePct: Double = 0.0,
    val thisMonthTxCount: Int = 0,
    val prevMonthTxCount: Int = 0
)

data class AppUpdateInfo(
    val latestVersion: String,
    val latestVersionCode: Int,
    val minSupportedVersion: String,
    val updateRequired: Boolean,
    val releaseNotes: String,
    val downloadUrl: String
) {
    fun isUpdateAvailable(currentVersionCode: Int): Boolean = latestVersionCode > currentVersionCode
    fun isMandatory(currentVersionCode: Int): Boolean = updateRequired && isUpdateAvailable(currentVersionCode)
}
