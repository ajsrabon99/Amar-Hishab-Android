package com.ajyra.amarhishab.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajyra.amarhishab.data.repository.FinanceRepository
import com.ajyra.amarhishab.model.MonthlyReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

class ReportsViewModel(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val calendar = Calendar.getInstance()

    private val _selectedYear = MutableStateFlow(calendar.get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private val _selectedMonth = MutableStateFlow(calendar.get(Calendar.MONTH) + 1)
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    val report: StateFlow<MonthlyReport> = combine(
        financeRepository.getAllTransactions(),
        _selectedYear,
        _selectedMonth
    ) { _, year, month ->
        financeRepository.getMonthlyReport(year, month)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = MonthlyReport(
            month = calendar.get(Calendar.MONTH) + 1,
            year = calendar.get(Calendar.YEAR),
            income = 0.0,
            expense = 0.0,
            saved = 0.0,
            categoryBreakdown = emptyList(),
            paymentMethodBreakdown = emptyList()
        )
    )

    fun setSelectedMonth(month: Int) {
        _selectedMonth.value = month
    }

    fun setSelectedYear(year: Int) {
        _selectedYear.value = year
    }
}
