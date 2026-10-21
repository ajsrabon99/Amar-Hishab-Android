package com.ajyra.amarhishab

import android.content.Context
import androidx.room.Room
import com.ajyra.amarhishab.data.local.AmarHishabDatabase
import com.ajyra.amarhishab.data.local.CategoryManager
import com.ajyra.amarhishab.data.local.EncryptedSessionManager
import com.ajyra.amarhishab.data.repository.FinanceRepository
import com.ajyra.amarhishab.model.AccountType
import com.ajyra.amarhishab.model.MonthlyComparison
import com.ajyra.amarhishab.model.TransactionType
import com.ajyra.amarhishab.model.Workspace
import com.ajyra.amarhishab.network.ApiClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class V127FeaturesUnitTest {

    private lateinit var context: Context
    private lateinit var database: AmarHishabDatabase
    private lateinit var sessionManager: EncryptedSessionManager
    private lateinit var categoryManager: CategoryManager
    private lateinit var repository: FinanceRepository

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, AmarHishabDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sessionManager = EncryptedSessionManager.getInstance(context)
        sessionManager.setWorkspace(Workspace.PERSONAL)
        categoryManager = CategoryManager.getInstance(context)
        val apiService = ApiClient.create(context)
        repository = FinanceRepository(
            transactionDao = database.transactionDao(),
            apiService = apiService,
            savingsGoalDao = database.savingsGoalDao(),
            sessionManager = sessionManager
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testDefaultCategoryDeleteAndPersistence() {
        val initialCategories = categoryManager.getAllCategories(TransactionType.EXPENSE, "PERSONAL")
        assertTrue("Initial expense categories should not be empty", initialCategories.isNotEmpty())

        val foodCat = initialCategories.first { it.id == "food" || it.nameEn.equals("Food", true) }

        // Delete default category for PERSONAL workspace
        val deleted = categoryManager.deleteCategory(foodCat.id, "PERSONAL")
        assertTrue("Deleting default category should return true", deleted)

        // Verify it disappears from active categories
        val updatedCategories = categoryManager.getAllCategories(TransactionType.EXPENSE, "PERSONAL")
        assertFalse("Deleted default category must not appear in active categories",
            updatedCategories.any { it.id == foodCat.id || it.nameEn.equals(foodCat.nameEn, true) })

        // Verify persistence after reload
        val newCategoryManager = CategoryManager.getInstance(context)
        val reloadedCategories = newCategoryManager.getAllCategories(TransactionType.EXPENSE, "PERSONAL")
        assertFalse("Deleted category must remain deleted across instances",
            reloadedCategories.any { it.id == foodCat.id || it.nameEn.equals(foodCat.nameEn, true) })
    }

    @Test
    fun testCustomCategoryCreateAndDelete() {
        val created = categoryManager.addCategory(
            name = "Test Custom Hobbies",
            type = TransactionType.EXPENSE,
            workspace = "PERSONAL"
        )
        assertNotNull(created)
        assertTrue(categoryManager.getAllCategories(TransactionType.EXPENSE, "PERSONAL")
            .any { it.nameEn == "Test Custom Hobbies" })

        // Delete custom category
        val deleted = categoryManager.deleteCategory(created.id, "PERSONAL")
        assertTrue("Deleting custom category should succeed", deleted)

        assertFalse("Deleted custom category must not be in active categories",
            categoryManager.getAllCategories(TransactionType.EXPENSE, "PERSONAL")
                .any { it.id == created.id })
    }

    @Test
    fun testExistingTransactionSurvivesCategoryDeletion() = runBlocking {
        // Add transaction with 'shopping'
        val addRes = repository.addTransaction(
            type = TransactionType.EXPENSE,
            amount = 1250.0,
            category = "shopping",
            accountCode = "cash",
            date = "2026-10-15",
            description = "Winter clothing"
        )
        val txId = (addRes as com.ajyra.amarhishab.network.NetworkResult.Success).data.id

        // Delete the 'shopping' category
        categoryManager.deleteCategory("shopping", "PERSONAL")

        // Transaction must still exist in database
        val fetchedTx = repository.getTransactionById(txId)
        assertNotNull("Historical transaction must not be deleted when category is deleted", fetchedTx)
        assertEquals(1250.0, fetchedTx!!.amount, 0.001)

        // Historical cache allows safe human-readable category name resolution
        val displayName = categoryManager.getCategoryDisplayName(fetchedTx.category, false)
        assertTrue("Category display name should be preserved from cache or fall back safely",
            displayName.isNotBlank())
    }

    @Test
    fun testWorkspaceSeparationOnCategoryDelete() {
        // Delete in PERSONAL
        categoryManager.deleteCategory("transport", "PERSONAL")

        // Must be deleted in PERSONAL
        val personalCategories = categoryManager.getAllCategories(TransactionType.EXPENSE, "PERSONAL")
        assertFalse(personalCategories.any { it.id == "transport" || it.nameEn.equals("Transport", true) })

        // Must NOT be deleted in BUSINESS
        val businessCategories = categoryManager.getAllCategories(TransactionType.EXPENSE, "BUSINESS")
        assertTrue("Deleting from Personal must not delete from Business workspace",
            businessCategories.any { it.id == "transport" || it.nameEn.equals("Transport", true) })
    }

    @Test
    fun testEditExpenseUpdatesOriginalAndRecalculates() = runBlocking {
        val addRes = repository.addTransaction(
            type = TransactionType.EXPENSE,
            amount = 500.0,
            category = "food",
            accountCode = AccountType.CASH.code,
            date = "2026-10-10",
            description = "Dinner"
        )
        val origId = (addRes as com.ajyra.amarhishab.network.NetworkResult.Success).data.id

        // Edit original transaction
        val editRes = repository.updateTransaction(
            id = origId,
            type = TransactionType.EXPENSE,
            amount = 850.0,
            category = "food",
            accountCode = AccountType.CASH.code,
            date = "2026-10-11",
            description = "Dinner with friends"
        )
        assertTrue(editRes is com.ajyra.amarhishab.network.NetworkResult.Success)
        val updatedTx = (editRes as com.ajyra.amarhishab.network.NetworkResult.Success).data

        // Verify ID is preserved (no duplicate transaction)
        assertEquals(origId, updatedTx.id)
        val allTx = repository.getAllTransactions().first()
        assertEquals("Only 1 transaction should exist", 1, allTx.size)
        assertEquals(850.0, allTx[0].amount, 0.001)
        assertEquals("Dinner with friends", allTx[0].description)

        // Verify summary recalculation
        val summary = repository.getFinancialSummary().first()
        assertEquals(850.0, summary.totalExpense, 0.001)
        assertEquals(-850.0, summary.cashBalance, 0.001)
    }

    @Test
    fun testEditIncomeUpdatesOriginalAndPreservesWorkspace() = runBlocking {
        sessionManager.setWorkspace(Workspace.BUSINESS)
        val addRes = repository.addTransaction(
            type = TransactionType.INCOME,
            amount = 25000.0,
            category = "salary",
            accountCode = AccountType.BANK.code,
            date = "2026-10-01",
            description = "Client Payment",
            workspace = "BUSINESS"
        )
        val origId = (addRes as com.ajyra.amarhishab.network.NetworkResult.Success).data.id

        // Edit the income
        repository.updateTransaction(
            id = origId,
            type = TransactionType.INCOME,
            amount = 32000.0,
            category = "freelance",
            accountCode = AccountType.BANK.code,
            date = "2026-10-02",
            description = "Revised Client Payment"
        )

        val updated = repository.getTransactionById(origId)
        assertNotNull(updated)
        assertEquals(origId, updated!!.id)
        assertEquals(32000.0, updated.amount, 0.001)
        assertEquals("BUSINESS", updated.workspace)

        val summary = repository.getFinancialSummary().first()
        assertEquals(32000.0, summary.totalIncome, 0.001)
        assertEquals(32000.0, summary.bankBalance, 0.001)
    }

    @Test
    fun testEditTransferRecalculatesSourceAndDestination() = runBlocking {
        // Initial income in Bank: 10,000
        repository.addTransaction(
            type = TransactionType.INCOME,
            amount = 10000.0,
            category = "deposit",
            accountCode = AccountType.BANK.code,
            date = "2026-10-01"
        )

        // Transfer 4,000 from Bank to bKash
        val transferRes = repository.transferMoney(
            fromAccount = AccountType.BANK.code,
            toAccount = AccountType.BKASH.code,
            amount = 4000.0,
            date = "2026-10-02",
            description = "Add money to bKash"
        )
        val txId = (transferRes as com.ajyra.amarhishab.network.NetworkResult.Success).data.id

        var summary = repository.getFinancialSummary().first()
        assertEquals(6000.0, summary.bankBalance, 0.001)
        assertEquals(4000.0, summary.bkashBalance, 0.001)

        // Edit transfer amount to 6,000 and change destination to Nagad
        repository.updateTransaction(
            id = txId,
            type = TransactionType.TRANSFER,
            amount = 6000.0,
            category = "transfer",
            accountCode = AccountType.BANK.code,
            toAccountCode = AccountType.NAGAD.code,
            date = "2026-10-03",
            description = "Add money to Nagad instead"
        )

        summary = repository.getFinancialSummary().first()
        assertEquals("Bank should have 10,000 - 6,000 = 4,000", 4000.0, summary.bankBalance, 0.001)
        assertEquals("bKash should have 0.0", 0.0, summary.bkashBalance, 0.001)
        assertEquals("Nagad should have 6,000.0", 6000.0, summary.nagadBalance, 0.001)
    }

    @Test
    fun testMonthlyComparisonCalculations() {
        val comparison = MonthlyComparison(
            thisMonthIncome = 50000.0,
            prevMonthIncome = 40000.0,
            incomeChangePct = 25.0,
            thisMonthExpense = 18000.0,
            prevMonthExpense = 20000.0,
            expenseChangePct = -10.0,
            thisMonthNet = 32000.0,
            prevMonthNet = 20000.0,
            netChangePct = 60.0,
            thisMonthTxCount = 12,
            prevMonthTxCount = 15
        )

        assertEquals(25.0, comparison.incomeChangePct, 0.001)
        assertEquals(-10.0, comparison.expenseChangePct, 0.001)
        assertEquals(32000.0, comparison.thisMonthNet, 0.001)
        assertEquals(12, comparison.thisMonthTxCount)
    }

    @Test
    fun testPeriodReportsFiltering() = runBlocking {
        val cal = Calendar.getInstance()
        val curYear = cal.get(Calendar.YEAR)
        val curMonth = cal.get(Calendar.MONTH) + 1
        val curMonthPrefix = String.format("%04d-%02d", curYear, curMonth)

        val prevCal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        val prevYear = prevCal.get(Calendar.YEAR)
        val prevMonth = prevCal.get(Calendar.MONTH) + 1
        val prevMonthPrefix = String.format("%04d-%02d", prevYear, prevMonth)

        // Current month transaction
        repository.addTransaction(
            type = TransactionType.INCOME,
            amount = 5000.0,
            category = "salary",
            accountCode = "cash",
            date = "$curMonthPrefix-05"
        )

        // Previous month transaction
        repository.addTransaction(
            type = TransactionType.INCOME,
            amount = 3000.0,
            category = "freelance",
            accountCode = "cash",
            date = "$prevMonthPrefix-10"
        )

        val thisMonthReport = repository.getPeriodReport("THIS_MONTH")
        assertEquals(5000.0, thisMonthReport.income, 0.001)

        val prevMonthReport = repository.getPeriodReport("PREVIOUS_MONTH")
        assertEquals(3000.0, prevMonthReport.income, 0.001)

        val thisYearReport = repository.getPeriodReport("THIS_YEAR")
        val expectedYearIncome = if (prevYear == curYear) 8000.0 else 5000.0
        assertEquals(expectedYearIncome, thisYearReport.income, 0.001)
    }
}
