package com.ajyra.amarhishab.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajyra.amarhishab.data.local.EncryptedSessionManager
import com.ajyra.amarhishab.data.repository.FinanceRepository
import com.ajyra.amarhishab.model.CategoryExpense
import com.ajyra.amarhishab.model.FinancialSummary
import com.ajyra.amarhishab.model.SavingsGoal
import com.ajyra.amarhishab.model.Transaction
import com.ajyra.amarhishab.model.User
import com.ajyra.amarhishab.network.NetworkResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val financeRepository: FinanceRepository,
    private val sessionManager: EncryptedSessionManager? = null
) : ViewModel() {

    val currentUser: StateFlow<User?> = sessionManager?.currentUser
        ?: MutableStateFlow<User?>(null).asStateFlow()

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
        refresh()
    }

    /**
     * Authoritative local refresh:
     * Room reactive flows (summary, recentTransactions, categoryExpenses, activeSavingsGoal)
     * automatically update the UI directly from SQLite.
     * Silent optional backend sync is attempted without blocking the user or displaying false errors when offline.
     */
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _syncMessage.value = null
            try {
                financeRepository.syncWithBackend()
            } catch (_: Exception) {
                // Ignore offline network errors during local dashboard refresh
            }
            delay(250) // Smooth refresh animation
            _isRefreshing.value = false
            _isInitialLoading.value = false
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }
}
