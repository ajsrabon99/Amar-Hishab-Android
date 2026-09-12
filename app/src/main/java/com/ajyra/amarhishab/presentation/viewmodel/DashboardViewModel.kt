package com.ajyra.amarhishab.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajyra.amarhishab.data.repository.FinanceRepository
import com.ajyra.amarhishab.model.FinancialSummary
import com.ajyra.amarhishab.model.Transaction
import com.ajyra.amarhishab.network.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val financeRepository: FinanceRepository
) : ViewModel() {

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

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            when (val res = financeRepository.syncWithBackend()) {
                is NetworkResult.Success -> {
                    _syncMessage.value = null
                }
                is NetworkResult.Error -> {
                    _syncMessage.value = res.messageBn
                }
                NetworkResult.Loading -> {}
            }
            _isRefreshing.value = false
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }
}
