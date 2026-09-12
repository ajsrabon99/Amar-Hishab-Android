package com.ajyra.amarhishab.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajyra.amarhishab.data.repository.FinanceRepository
import com.ajyra.amarhishab.model.TransactionType
import com.ajyra.amarhishab.network.NetworkResult
import com.ajyra.amarhishab.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SaveState {
    object Idle : SaveState()
    object Saving : SaveState()
    object Success : SaveState()
    data class Error(val messageEn: String, val messageBn: String) : SaveState()
}

class AddTransactionViewModel(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    fun saveTransaction(
        type: TransactionType,
        amountStr: String,
        category: String,
        accountCode: String,
        dateStr: String = DateUtils.todayDateString(),
        description: String? = null
    ) {
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _saveState.value = SaveState.Error(
                messageEn = "Please enter a valid amount greater than zero.",
                messageBn = "অনুগ্রহ করে শূন্যের বেশি সঠিক পরিমাণ টাকা উল্লেখ করুন।"
            )
            return
        }
        if (category.isBlank()) {
            _saveState.value = SaveState.Error(
                messageEn = "Please select a category.",
                messageBn = "অনুগ্রহ করে একটি খাত নির্বাচন করুন।"
            )
            return
        }

        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            when (val res = financeRepository.addTransaction(
                type = type,
                amount = amount,
                category = category,
                accountCode = accountCode,
                date = dateStr,
                description = description
            )) {
                is NetworkResult.Success -> {
                    _saveState.value = SaveState.Success
                }
                is NetworkResult.Error -> {
                    _saveState.value = SaveState.Error(
                        messageEn = res.messageEn,
                        messageBn = res.messageBn
                    )
                }
                NetworkResult.Loading -> {}
            }
        }
    }

    fun resetState() {
        _saveState.value = SaveState.Idle
    }
}
