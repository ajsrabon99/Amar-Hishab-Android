package com.ajyra.amarhishab.presentation.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ajyra.amarhishab.model.AccountType
import com.ajyra.amarhishab.presentation.components.AccountBalancePill
import com.ajyra.amarhishab.presentation.components.EmptyStateView
import com.ajyra.amarhishab.presentation.components.HeroBalanceCard
import com.ajyra.amarhishab.presentation.components.TransactionRowItem
import com.ajyra.amarhishab.presentation.viewmodel.DashboardViewModel
import com.ajyra.amarhishab.ui.theme.EmeraldPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    dashboardViewModel: DashboardViewModel,
    isBengali: Boolean,
    onNavigateToAddTransaction: (isExpense: Boolean) -> Unit,
    onNavigateToTransfer: () -> Unit,
    onNavigateToTransactions: () -> Unit
) {
    val summary by dashboardViewModel.summary.collectAsState()
    val recentTransactions by dashboardViewModel.recentTransactions.collectAsState()
    val isRefreshing by dashboardViewModel.isRefreshing.collectAsState()
    val syncMessage by dashboardViewModel.syncMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(syncMessage) {
        syncMessage?.let {
            snackbarHostState.showSnackbar(it)
            dashboardViewModel.clearSyncMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isBengali) "আমার হিসাব" else "Amar Hishab",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                        Text(
                            text = if (isBengali) "আর্থিক ড্যাশবোর্ড" else "Financial Dashboard",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { dashboardViewModel.refresh() },
                        enabled = !isRefreshing,
                        modifier = Modifier.testTag("dashboard_sync_button")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = EmeraldPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = if (isBengali) "সিঙ্ক করুন" else "Sync",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("home_screen_scroll"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Balance Card
            item {
                HeroBalanceCard(
                    totalBalance = summary.totalBalance,
                    totalIncome = summary.totalIncome,
                    totalExpense = summary.totalExpense,
                    isBengali = isBengali,
                    onIncomeClick = { onNavigateToAddTransaction(false) },
                    onExpenseClick = { onNavigateToAddTransaction(true) },
                    onTransferClick = onNavigateToTransfer
                )
            }

            // Accounts breakdown horizontal scroll
            item {
                Column {
                    Text(
                        text = if (isBengali) "অ্যাকাউন্ট ব্যালেন্স" else "Accounts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AccountBalancePill(
                            accountType = AccountType.CASH,
                            balance = summary.cashBalance,
                            isBengali = isBengali
                        )
                        AccountBalancePill(
                            accountType = AccountType.BANK,
                            balance = summary.bankBalance,
                            isBengali = isBengali
                        )
                        AccountBalancePill(
                            accountType = AccountType.BKASH,
                            balance = summary.bkashBalance,
                            isBengali = isBengali
                        )
                        AccountBalancePill(
                            accountType = AccountType.NAGAD,
                            balance = summary.nagadBalance,
                            isBengali = isBengali
                        )
                    }
                }
            }

            // Recent Transactions header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBengali) "সাম্প্রতিক লেনদেন" else "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(
                        onClick = onNavigateToTransactions,
                        modifier = Modifier.testTag("home_view_all_transactions")
                    ) {
                        Text(
                            text = if (isBengali) "সব দেখুন" else "View All",
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Recent Transactions List
            if (recentTransactions.isEmpty()) {
                item {
                    EmptyStateView(
                        title = if (isBengali) "কোনো লেনদেন নেই" else "No Recent Transactions",
                        subtitle = if (isBengali)
                            "নতুন আয় বা ব্যয় যোগ করে আপনার হিসাব শুরু করুন"
                        else
                            "Start managing your money by adding an income or expense",
                        actionLabel = if (isBengali) "+ লেনদেন যোগ করুন" else "+ Add Transaction",
                        onActionClick = { onNavigateToAddTransaction(true) }
                    )
                }
            } else {
                items(recentTransactions, key = { it.id }) { transaction ->
                    TransactionRowItem(
                        transaction = transaction,
                        isBengali = isBengali,
                        onClick = onNavigateToTransactions
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
