package com.ajyra.amarhishab.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajyra.amarhishab.data.repository.FinanceRepository
import com.ajyra.amarhishab.model.Transaction
import com.ajyra.amarhishab.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOption {
    DATE_DESC,
    DATE_ASC,
    AMOUNT_DESC,
    AMOUNT_ASC
}

data class TransactionFilters(
    val type: TransactionType? = null,
    val category: String? = null,
    val account: String? = null,
    val searchQuery: String? = null,
    val sortOption: SortOption? = null
)

class TransactionsViewModel(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val allTransactions: Flow<List<Transaction>> = financeRepository.getAllTransactions()

    private val _filters = MutableStateFlow(TransactionFilters())
    val filters: StateFlow<TransactionFilters> = _filters.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    val filteredTransactions: StateFlow<List<Transaction>> = combine(allTransactions, _filters) { list, filter ->
        var res = list

        if (filter.type != null) {
            res = res.filter { it.type == filter.type }
        }
        if (!filter.category.isNullOrBlank()) {
            res = res.filter { it.category.equals(filter.category, ignoreCase = true) }
        }
        if (!filter.account.isNullOrBlank()) {
            res = res.filter { it.accountCode.equals(filter.account, ignoreCase = true) || it.toAccountCode.equals(filter.account, ignoreCase = true) }
        }
        if (!filter.searchQuery.isNullOrBlank()) {
            val q = filter.searchQuery.lowercase()
            res = res.filter {
                it.category.lowercase().contains(q) ||
                it.accountCode.lowercase().contains(q) ||
                (it.description?.lowercase()?.contains(q) == true)
            }
        }
        when (filter.sortOption ?: SortOption.DATE_DESC) {
            SortOption.DATE_DESC -> res.sortedByDescending { it.date }
            SortOption.DATE_ASC -> res.sortedBy { it.date }
            SortOption.AMOUNT_DESC -> res.sortedByDescending { it.amount }
            SortOption.AMOUNT_ASC -> res.sortedBy { it.amount }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    fun setFilterType(type: TransactionType?) {
        _filters.value = _filters.value.copy(type = type)
    }

    fun setFilterCategory(cat: String?) {
        _filters.value = _filters.value.copy(category = cat)
    }

    fun setFilterAccount(acc: String?) {
        _filters.value = _filters.value.copy(account = acc)
    }

    fun setSearchQuery(query: String) {
        _filters.value = _filters.value.copy(searchQuery = query)
    }

    fun setSortOption(sort: SortOption) {
        _filters.value = _filters.value.copy(sortOption = sort)
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            financeRepository.deleteTransaction(id)
            _actionMessage.value = "Transaction deleted"
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }
}
