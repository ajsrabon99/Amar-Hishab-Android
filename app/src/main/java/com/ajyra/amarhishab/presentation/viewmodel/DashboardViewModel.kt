package com.ajyra.amarhishab.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajyra.amarhishab.data.local.EncryptedSessionManager
import com.ajyra.amarhishab.data.repository.FinanceRepository
import com.ajyra.amarhishab.model.CategoryExpense
import com.ajyra.amarhishab.model.FinancialSummary
import com.ajyra.amarhishab.model.MonthlyComparison
import com.ajyra.amarhishab.model.SavingsGoal
import com.ajyra.amarhishab.model.Transaction
import com.ajyra.amarhishab.model.User
import com.ajyra.amarhishab.network.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class DashboardViewModel(
    private val financeRepository: FinanceRepository,
    private val sessionManager: EncryptedSessionManager? = null
) : ViewModel() {

    val currentUser: StateFlow<User?> = sessionManager?.currentUser
        ?: MutableStateFlow<User?>(null).asStateFlow()

    val activeWorkspace: StateFlow<com.ajyra.amarhishab.model.Workspace> = sessionManager?.currentWorkspace
        ?: MutableStateFlow(com.ajyra.amarhishab.model.Workspace.PERSONAL).asStateFlow()

    fun switchWorkspace(workspace: com.ajyra.amarhishab.model.Workspace) {
        sessionManager?.setWorkspace(workspace)
        com.ajyra.amarhishab.analytics.AnalyticsManager.logWorkspaceSwitched(workspace == com.ajyra.amarhishab.model.Workspace.BUSINESS)
        refresh()
    }

    val isBalanceHidden: StateFlow<Boolean> = sessionManager?.isBalanceHidden
        ?: MutableStateFlow(false).asStateFlow()

    fun toggleBalanceVisibility() {
        sessionManager?.let { sm ->
            sm.setBalanceHidden(!sm.isBalanceHidden())
        }
    }

    val summary: StateFlow<FinancialSummary> = financeRepository.getFinancialSummary()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = FinancialSummary()
        )

    val monthlyComparison: StateFlow<MonthlyComparison> = financeRepository.getMonthlyComparison()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = MonthlyComparison()
        )

    val recentTransactions: StateFlow<List<Transaction>> = financeRepository.getRecentTransactions(8)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val categoryExpenses: StateFlow<List<CategoryExpense>> = financeRepository.getCategoryExpenses()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val activeSavingsGoal: StateFlow<SavingsGoal?> = financeRepository.getActiveSavingsGoal()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )

    val allSavingsGoals: StateFlow<List<SavingsGoal>> = financeRepository.getAllSavingsGoals()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun setActiveGoal(id: String) {
        viewModelScope.launch {
            financeRepository.setActiveDashboardGoal(id)
        }
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isInitialLoading = MutableStateFlow(true)
    val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    init {
        // Fast-path: Render immediately from local SQLite Room cache.
        // As soon as Room produces the local financial summary, dismiss initial skeleton instantly.
        viewModelScope.launch {
            financeRepository.getFinancialSummary().collect {
                _isInitialLoading.value = false
            }
        }
        // Safety guard: Never block skeleton for more than 350ms even on brand new databases
        viewModelScope.launch {
            delay(350)
            _isInitialLoading.value = false
        }
        // Silent background remote sync runs strictly asynchronously without blocking UI startup
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withTimeoutOrNull(4000L) {
                    financeRepository.syncWithBackend()
                }
            } catch (_: Exception) {
                // Ignore offline network errors
            }
        }
    }

    /**
     * User-triggered pull-to-refresh:
     * Room reactive flows automatically reflect any SQLite updates.
     * Remote sync uses a strict 4-second timeout so pull-to-refresh remains snappy.
     */
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _syncMessage.value = null
            try {
                withTimeoutOrNull(4000L) {
                    financeRepository.syncWithBackend()
                }
            } catch (_: Exception) {
                // Ignore network errors during offline refresh
            }
            delay(150)
            _isRefreshing.value = false
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }
}
