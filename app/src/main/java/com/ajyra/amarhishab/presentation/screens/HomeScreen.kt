package com.ajyra.amarhishab.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ajyra.amarhishab.R
import com.ajyra.amarhishab.model.AccountType
import com.ajyra.amarhishab.presentation.components.AccountBalancePill
import com.ajyra.amarhishab.presentation.components.DashboardSkeleton
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
    unreadNotificationCount: Int = 0,
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAddTransaction: (isExpense: Boolean) -> Unit,
    onNavigateToTransfer: () -> Unit,
    onNavigateToTransactions: () -> Unit
) {
    val summary by dashboardViewModel.summary.collectAsState()
    val recentTransactions by dashboardViewModel.recentTransactions.collectAsState()
    val isRefreshing by dashboardViewModel.isRefreshing.collectAsState()
    val isInitialLoading by dashboardViewModel.isInitialLoading.collectAsState()
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.amar_hishab_icon),
                            contentDescription = "Amar Hishab Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
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
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToNotifications,
                        modifier = Modifier.testTag("dashboard_notifications_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotificationCount > 0) {
                                    Badge(
                                        containerColor = EmeraldPrimary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text(text = if (unreadNotificationCount > 9) "9+" else unreadNotificationCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = if (isBengali) "বিজ্ঞপ্তি" else "Notifications",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("dashboard_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = if (isBengali) "সেটিংস" else "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (isInitialLoading) {
            DashboardSkeleton(
                modifier = Modifier.padding(innerPadding)
            )
        } else {
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

            // Net Cash Flow Health Card
            item {
                val netFlow = summary.totalIncome - summary.totalExpense
                val savingsRate = if (summary.totalIncome > 0) {
                    ((netFlow / summary.totalIncome) * 100).coerceIn(0.0, 100.0).toInt()
                } else 0

                androidx.compose.material3.Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isBengali) "ক্যাশ ফ্লো স্ট্যাটাস" else "Cash Flow Overview",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            androidx.compose.material3.Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (netFlow >= 0) com.ajyra.amarhishab.ui.theme.IncomeGreen.copy(alpha = 0.15f)
                                        else com.ajyra.amarhishab.ui.theme.ExpenseRed.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (netFlow >= 0) {
                                        if (isBengali) "+ উদ্ধৃত্ত" else "+ Surplus"
                                    } else {
                                        if (isBengali) "- ঘাটতি" else "- Deficit"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (netFlow >= 0) com.ajyra.amarhishab.ui.theme.IncomeGreen else com.ajyra.amarhishab.ui.theme.ExpenseRed,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = if (isBengali) "নিট ক্যাশ ফ্লো" else "Net Savings",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = com.ajyra.amarhishab.utils.CurrencyFormatter.format(netFlow, isBengali),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (netFlow >= 0) com.ajyra.amarhishab.ui.theme.IncomeGreen else com.ajyra.amarhishab.ui.theme.ExpenseRed
                                )
                            }

                            if (summary.totalIncome > 0) {
                                Text(
                                    text = if (isBengali) "সঞ্চয় হার: ${com.ajyra.amarhishab.utils.CurrencyFormatter.toBengaliDigits(savingsRate.toString())}%"
                                           else "Savings Rate: $savingsRate%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (summary.totalIncome > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = { (savingsRate / 100f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = com.ajyra.amarhishab.ui.theme.FintechPrimary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
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
}
