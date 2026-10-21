package com.ajyra.amarhishab.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajyra.amarhishab.data.local.EncryptedSessionManager
import com.ajyra.amarhishab.data.local.SpendingPlannerManager
import com.ajyra.amarhishab.data.repository.FinanceRepository
import com.ajyra.amarhishab.model.SmartSpendingSummary
import com.ajyra.amarhishab.model.Transaction
import com.ajyra.amarhishab.model.Workspace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SmartSpendingViewModel(
    private val spendingPlannerManager: SpendingPlannerManager,
    private val financeRepository: FinanceRepository,
    private val sessionManager: EncryptedSessionManager
) : ViewModel() {

    private val _manualRefreshTrigger = MutableStateFlow(0)

    val summary: StateFlow<SmartSpendingSummary> = combine(
        financeRepository.getAllTransactions(),
        sessionManager.currentWorkspace,
        sessionManager.language,
        _manualRefreshTrigger
    ) { transactions: List<Transaction>, workspace: Workspace, lang: String, _: Int ->
        val isBengali = lang == "bn"
        spendingPlannerManager.computeSmartSpending(
            allTransactions = transactions,
            workspace = workspace,
            isBengali = isBengali
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = SmartSpendingSummary()
    )

    fun setMonthlyLimit(limit: Double) {
        val currentWorkspace = sessionManager.getSavedWorkspace()
        spendingPlannerManager.setMonthlyLimit(currentWorkspace, limit)
        _manualRefreshTrigger.value += 1
    }

    fun toggleRecurringLimit(enabled: Boolean) {
        val currentWorkspace = sessionManager.getSavedWorkspace()
        spendingPlannerManager.setRecurringLimitEnabled(currentWorkspace, enabled)
        _manualRefreshTrigger.value += 1
    }

    fun refresh() {
        _manualRefreshTrigger.value += 1
    }
}
