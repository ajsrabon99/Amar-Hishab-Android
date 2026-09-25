package com.ajyra.amarhishab.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajyra.amarhishab.data.repository.FinanceRepository
import com.ajyra.amarhishab.model.SavingsGoal
import com.ajyra.amarhishab.model.SmartSavingsPlan
import com.ajyra.amarhishab.utils.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SavingsGoalsViewModel(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    val goals: StateFlow<List<SavingsGoal>> = financeRepository.getAllSavingsGoals()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val activeGoal: StateFlow<SavingsGoal?> = financeRepository.getActiveSavingsGoal()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )

    private val _selectedGoal = MutableStateFlow<SavingsGoal?>(null)
    val selectedGoal: StateFlow<SavingsGoal?> = _selectedGoal.asStateFlow()

    fun selectGoal(goal: SavingsGoal?) {
        _selectedGoal.value = goal
    }

    val smartSavingsPlan: StateFlow<SmartSavingsPlan?> = _selectedGoal.flatMapLatest { currentSelected ->
        val target = currentSelected ?: activeGoal.value
        if (target != null) {
            financeRepository.getSmartSavingsPlan(target)
        } else {
            flowOf(null)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = null
    )

    fun createGoal(
        name: String,
        targetAmountStr: String,
        targetDate: String,
        description: String? = null,
        iconCategory: String? = "general",
        onResult: (success: Boolean, errorBn: String?, errorEn: String?) -> Unit
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            onResult(false, "লক্ষ্যের নাম খালি হতে পারে না।", "Goal name cannot be empty.")
            return
        }

        val targetAmount = targetAmountStr.toDoubleOrNull()
        if (targetAmount == null || targetAmount <= 0.0) {
            onResult(false, "টার্গেট পরিমাণ শূন্যের বেশি হতে হবে।", "Target amount must be greater than zero.")
            return
        }

        val tempGoal = SavingsGoal(
            id = "",
            name = trimmedName,
            targetAmount = targetAmount,
            targetDate = targetDate
        )
        if (tempGoal.getDaysRemaining() <= 0) {
            onResult(false, "টার্গেট তারিখ ভবিষ্যতের হতে হবে।", "Target date must be in the future.")
            return
        }

        viewModelScope.launch {
            try {
                val newGoal = financeRepository.createSavingsGoal(
                    name = trimmedName,
                    targetAmount = targetAmount,
                    targetDate = targetDate,
                    description = description,
                    iconCategory = iconCategory
                )
                _selectedGoal.value = newGoal
                onResult(true, null, null)
            } catch (e: Exception) {
                onResult(false, "লক্ষ্য তৈরি করা যায় নি: ${e.message}", "Failed to create goal: ${e.message}")
            }
        }
    }

    fun updateGoal(
        goal: SavingsGoal,
        name: String,
        targetAmountStr: String,
        targetDate: String,
        description: String? = null,
        iconCategory: String? = null,
        onResult: (success: Boolean, errorBn: String?, errorEn: String?) -> Unit
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            onResult(false, "লক্ষ্যের নাম খালি হতে পারে না।", "Goal name cannot be empty.")
            return
        }

        val targetAmount = targetAmountStr.toDoubleOrNull()
        if (targetAmount == null || targetAmount <= 0.0) {
            onResult(false, "টার্গেট পরিমাণ শূন্যের বেশি হতে হবে।", "Target amount must be greater than zero.")
            return
        }

        viewModelScope.launch {
            try {
                val updated = goal.copy(
                    name = trimmedName,
                    targetAmount = targetAmount,
                    targetDate = targetDate,
                    description = description,
                    iconCategory = iconCategory ?: goal.iconCategory
                )
                financeRepository.updateSavingsGoal(updated)
                if (_selectedGoal.value?.id == goal.id) {
                    _selectedGoal.value = updated
                }
                onResult(true, null, null)
            } catch (e: Exception) {
                onResult(false, "আপডেট ব্যর্থ হয়েছে: ${e.message}", "Failed to update goal: ${e.message}")
            }
        }
    }

    fun deleteGoal(id: String) {
        viewModelScope.launch {
            financeRepository.deleteSavingsGoal(id)
            if (_selectedGoal.value?.id == id) {
                _selectedGoal.value = null
            }
        }
    }

    fun addMoneyToGoal(
        goalId: String,
        amountStr: String,
        onResult: (success: Boolean, errorBn: String?, errorEn: String?) -> Unit
    ) {
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            onResult(false, "সঠিক পরিমাণ টাকা উল্লেখ করুন।", "Please enter a valid amount.")
            return
        }

        viewModelScope.launch {
            try {
                financeRepository.addMoneyToGoal(goalId, amount)
                onResult(true, null, null)
            } catch (e: Exception) {
                onResult(false, "টাকা যোগ করা যায় নি: ${e.message}", "Failed to add money: ${e.message}")
            }
        }
    }

    fun setActiveDashboardGoal(id: String) {
        viewModelScope.launch {
            financeRepository.setActiveDashboardGoal(id)
        }
    }
}
