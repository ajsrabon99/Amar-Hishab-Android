package com.ajyra.amarhishab

import android.content.Context
import com.ajyra.amarhishab.data.local.SpendingPlannerManager
import com.ajyra.amarhishab.model.AccountType
import com.ajyra.amarhishab.model.DailySpendingStatus
import com.ajyra.amarhishab.model.Transaction
import com.ajyra.amarhishab.model.TransactionType
import com.ajyra.amarhishab.model.Workspace
import com.ajyra.amarhishab.utils.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SmartSpendingUnitTest {

    private lateinit var context: Context
    private lateinit var plannerManager: SpendingPlannerManager

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        SpendingPlannerManager.resetForTesting()
        plannerManager = SpendingPlannerManager.getInstance(context)
        plannerManager.clearLimitsForTesting()
    }

    @Test
    fun testNewUserStartsAtZeroLimitAndUnconfigured() {
        assertEquals(0.0, plannerManager.getMonthlyLimit(Workspace.PERSONAL), 0.01)
        assertEquals(0.0, plannerManager.getMonthlyLimit(Workspace.BUSINESS), 0.01)
        assertFalse(plannerManager.isLimitConfigured(Workspace.PERSONAL))

        val summary = plannerManager.computeSmartSpending(emptyList(), Workspace.PERSONAL, false)
        assertEquals(0.0, summary.monthlyLimit, 0.01)
        assertFalse(summary.isConfigured)
        assertEquals(0.0, summary.suggestedLimit, 0.01)
    }

    @Test
    fun testIntelligentBudgetSuggestionFromActualExpenses() {
        val today = DateUtils.todayDateString()
        val txs = listOf(
            Transaction(
                id = "tx1",
                type = TransactionType.EXPENSE,
                amount = 4000.0,
                category = "Groceries",
                accountCode = "CASH",
                date = today,
                workspace = Workspace.PERSONAL.id
            ),
            Transaction(
                id = "tx2",
                type = TransactionType.EXPENSE,
                amount = 6000.0,
                category = "Rent",
                accountCode = "BANK",
                date = today,
                workspace = Workspace.PERSONAL.id
            ),
            // Transfers MUST be ignored in suggestion calculation
            Transaction(
                id = "tx3",
                type = TransactionType.TRANSFER,
                amount = 20000.0,
                category = "Transfer",
                accountCode = "BANK",
                toAccountCode = "BKASH",
                date = today,
                workspace = Workspace.PERSONAL.id
            )
        )

        val suggested = plannerManager.calculateSuggestedMonthlyLimit(txs, Workspace.PERSONAL)
        // Expenses total 10,000 in 1 month -> suggested rounded to nearest 500 = 10,000.0
        assertEquals(10000.0, suggested, 0.01)

        val summary = plannerManager.computeSmartSpending(txs, Workspace.PERSONAL, false)
        assertEquals(0.0, summary.monthlyLimit, 0.01)
        assertFalse(summary.isConfigured)
        assertEquals(10000.0, summary.suggestedLimit, 0.01)
    }

    @Test
    fun testWorkspaceIsolationForSpendingLimits() {
        plannerManager.setMonthlyLimit(Workspace.PERSONAL, 30000.0)
        plannerManager.setMonthlyLimit(Workspace.BUSINESS, 75000.0)

        assertEquals(30000.0, plannerManager.getMonthlyLimit(Workspace.PERSONAL), 0.01)
        assertEquals(75000.0, plannerManager.getMonthlyLimit(Workspace.BUSINESS), 0.01)
    }

    @Test
    fun testDynamicDailyCalculationExcludesIncomeAndTransfer() {
        val today = DateUtils.todayDateString()
        plannerManager.setMonthlyLimit(Workspace.PERSONAL, 30000.0)

        val transactions = listOf(
            // Expense that SHOULD count
            Transaction(
                id = "tx1",
                type = TransactionType.EXPENSE,
                amount = 5000.0,
                category = "Food",
                accountCode = "CASH",
                date = today,
                workspace = Workspace.PERSONAL.id
            ),
            // Income that MUST NOT reduce budget
            Transaction(
                id = "tx2",
                type = TransactionType.INCOME,
                amount = 20000.0,
                category = "Salary",
                accountCode = "BANK",
                date = today,
                workspace = Workspace.PERSONAL.id
            ),
            // Transfer that MUST NOT reduce budget
            Transaction(
                id = "tx3",
                type = TransactionType.TRANSFER,
                amount = 8000.0,
                category = "Transfer",
                accountCode = "CASH",
                toAccountCode = "BKASH",
                date = today,
                workspace = Workspace.PERSONAL.id
            ),
            // Business expense that MUST NOT affect Personal budget
            Transaction(
                id = "tx4",
                type = TransactionType.EXPENSE,
                amount = 15000.0,
                category = "Office Supplies",
                accountCode = "BANK",
                date = today,
                workspace = Workspace.BUSINESS.id
            )
        )

        val personalSummary = plannerManager.computeSmartSpending(
            allTransactions = transactions,
            workspace = Workspace.PERSONAL,
            isBengali = false
        )

        // Only the 5,000 expense should count
        assertEquals(30000.0, personalSummary.monthlyLimit, 0.01)
        assertEquals(5000.0, personalSummary.totalSpent, 0.01)
        assertEquals(25000.0, personalSummary.remainingBudget, 0.01)
        assertEquals(5000.0, personalSummary.todayActualSpending, 0.01)

        val cal = Calendar.getInstance()
        val totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val curDay = cal.get(Calendar.DAY_OF_MONTH)
        val remainingDays = totalDays - curDay + 1
        val expectedDaily = 25000.0 / remainingDays
        assertEquals(expectedDaily, personalSummary.recommendedDailySpending, 0.01)

        // Business workspace check: should have 15,000 spent
        val businessSummary = plannerManager.computeSmartSpending(
            allTransactions = transactions,
            workspace = Workspace.BUSINESS,
            isBengali = false
        )
        assertEquals(15000.0, businessSummary.totalSpent, 0.01)
    }

    @Test
    fun testAlertThresholds() {
        val cal = Calendar.getInstance()
        val totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val curDay = cal.get(Calendar.DAY_OF_MONTH)
        val remainingDays = totalDays - curDay + 1

        val today = DateUtils.todayDateString()
        val limit = 30000.0
        plannerManager.setMonthlyLimit(Workspace.PERSONAL, limit)

        // Case A: Spending below recommended -> NORMAL
        val txNormal = listOf(
            Transaction(
                id = "t_norm",
                type = TransactionType.EXPENSE,
                amount = 200.0,
                category = "Food",
                accountCode = "CASH",
                date = today,
                workspace = Workspace.PERSONAL.id
            )
        )
        val summaryNormal = plannerManager.computeSmartSpending(txNormal, Workspace.PERSONAL, false)
        assertEquals(DailySpendingStatus.NORMAL, summaryNormal.todayStatus)

        // Case B: Category breakdown contains real categories
        assertNotNull(summaryNormal.categoryBreakdown)
        assertTrue(summaryNormal.categoryBreakdown.any { it.category == "Food" })
        assertEquals("Food", summaryNormal.highestCategory)
    }
}
