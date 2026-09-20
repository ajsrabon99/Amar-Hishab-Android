package com.ajyra.amarhishab.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ajyra.amarhishab.data.local.AmarHishabDatabase
import com.ajyra.amarhishab.data.local.EncryptedSessionManager
import com.ajyra.amarhishab.data.repository.FinanceRepository
import com.ajyra.amarhishab.network.AmarHishabApiService
import com.ajyra.amarhishab.network.ApiClient

class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    private val sessionManager: EncryptedSessionManager by lazy {
        EncryptedSessionManager.getInstance(context)
    }

    private val apiService: AmarHishabApiService by lazy {
        ApiClient.create(context)
    }

    private val database: AmarHishabDatabase by lazy {
        AmarHishabDatabase.getInstance(context)
    }

    private val financeRepository: FinanceRepository by lazy {
        FinanceRepository(database.transactionDao(), apiService)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> {
                DashboardViewModel(financeRepository) as T
            }
            modelClass.isAssignableFrom(TransactionsViewModel::class.java) -> {
                TransactionsViewModel(financeRepository) as T
            }
            modelClass.isAssignableFrom(AddTransactionViewModel::class.java) -> {
                AddTransactionViewModel(financeRepository) as T
            }
            modelClass.isAssignableFrom(TransferViewModel::class.java) -> {
                TransferViewModel(financeRepository) as T
            }
            modelClass.isAssignableFrom(ReportsViewModel::class.java) -> {
                ReportsViewModel(financeRepository) as T
            }
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
                ProfileViewModel(financeRepository, sessionManager) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(sessionManager, com.ajyra.amarhishab.data.local.NotificationRepository.getInstance(context)) as T
            }
            modelClass.isAssignableFrom(NotificationViewModel::class.java) -> {
                NotificationViewModel(com.ajyra.amarhishab.data.local.NotificationRepository.getInstance(context)) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
