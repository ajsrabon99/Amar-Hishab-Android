package com.ajyra.amarhishab.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajyra.amarhishab.data.repository.FinanceRepository
import com.ajyra.amarhishab.network.NetworkResult
import com.ajyra.amarhishab.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TransferViewModel(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    fun transferMoney(
        fromAccount: String,
        toAccount: String,
        amountStr: String,
        dateStr: String = DateUtils.todayDateString(),
        description: String? = null
    ) {
        if (fromAccount.equals(toAccount, ignoreCase = true)) {
            _saveState.value = SaveState.Error(
                messageEn = "Source and destination accounts must be different.",
                messageBn = "উৎস এবং গন্তব্য অ্যাকাউন্ট ভিন্ন হতে হবে।"
            )
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _saveState.value = SaveState.Error(
                messageEn = "Please enter a valid transfer amount greater than zero.",
                messageBn = "অনুগ্রহ করে শূন্যের বেশি সঠিক পরিমাণ টাকা উল্লেখ করুন।"
            )
            return
        }

        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            when (val res = financeRepository.transferMoney(
                fromAccount = fromAccount,
                toAccount = toAccount,
                amount = amount,
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
