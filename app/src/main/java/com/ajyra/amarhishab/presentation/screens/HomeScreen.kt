package com.ajyra.amarhishab.presentation.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ajyra.amarhishab.R
import com.ajyra.amarhishab.model.AccountType
import com.ajyra.amarhishab.model.CategoryExpense
import com.ajyra.amarhishab.presentation.components.AccountBalancePill
import com.ajyra.amarhishab.presentation.components.DashboardSkeleton
import com.ajyra.amarhishab.presentation.components.EmptyStateView
import com.ajyra.amarhishab.presentation.components.HeroBalanceCard
import com.ajyra.amarhishab.presentation.components.SmartSpendingCard
import com.ajyra.amarhishab.presentation.components.TransactionRowItem
import com.ajyra.amarhishab.presentation.viewmodel.DashboardViewModel
import com.ajyra.amarhishab.presentation.viewmodel.SmartSpendingViewModel
import com.ajyra.amarhishab.ui.theme.EmeraldPrimary
import com.ajyra.amarhishab.ui.theme.ExpenseRed
import com.ajyra.amarhishab.ui.theme.FintechPrimary
import com.ajyra.amarhishab.ui.theme.IncomeGreen
import com.ajyra.amarhishab.utils.CurrencyFormatter
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    dashboardViewModel: DashboardViewModel,
    isBengali: Boolean,
    unreadNotificationCount: Int = 0,
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToStatement: () -> Unit = {},
    onNavigateToImportData: () -> Unit = {},
    onNavigateToCustomCategories: () -> Unit = {},
    onNavigateToSavingsGoals: () -> Unit = {},
    smartSpendingViewModel: SmartSpendingViewModel? = null,
    onNavigateToSmartSpending: () -> Unit = {},
    onNavigateToAddTransaction: (isExpense: Boolean) -> Unit,
    onNavigateToTransfer: () -> Unit,
    onNavigateToTransactions: () -> Unit
) {
    val summary by dashboardViewModel.summary.collectAsState()
    val monthlyComparison by dashboardViewModel.monthlyComparison.collectAsState()
    val recentTransactions by dashboardViewModel.recentTransactions.collectAsState()
    val categoryExpenses by dashboardViewModel.categoryExpenses.collectAsState()
    val activeGoal by dashboardViewModel.activeSavingsGoal.collectAsState()
    val isRefreshing by dashboardViewModel.isRefreshing.collectAsState()
    val isInitialLoading by dashboardViewModel.isInitialLoading.collectAsState()
    val syncMessage by dashboardViewModel.syncMessage.collectAsState()
    val currentUser by dashboardViewModel.currentUser.collectAsState()
    val activeWorkspace by dashboardViewModel.activeWorkspace.collectAsState()
    val isBalanceHidden by dashboardViewModel.isBalanceHidden.collectAsState()
    val smartSpendingSummary by (smartSpendingViewModel?.summary ?: remember { kotlinx.coroutines.flow.MutableStateFlow(com.ajyra.amarhishab.model.SmartSpendingSummary()) }).collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showWorkspaceSwitchDialog by remember { mutableStateOf(false) }

    val avatarUri = currentUser?.avatarUri
    val avatarBitmap = remember(avatarUri) {
        if (!avatarUri.isNullOrBlank()) {
            try {
                val f = File(avatarUri)
                if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
            } catch (_: Exception) {
                null
            }
        } else null
    }

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
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showWorkspaceSwitchDialog = true }
                            .testTag("dashboard_profile_avatar")
                    ) {
                        if (avatarBitmap != null) {
                            Image(
                                bitmap = avatarBitmap.asImageBitmap(),
                                contentDescription = "Profile Photo",
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.amar_hishab_icon),
                                contentDescription = "Amar Hishab Logo",
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (activeWorkspace == com.ajyra.amarhishab.model.Workspace.BUSINESS) {
                                        if (isBengali) "ব্যবসায়িক হিসাব" else "Business Account"
                                    } else {
                                        currentUser?.displayName?.ifBlank { null } ?: (if (isBengali) "ব্যক্তিগত হিসাব" else "Personal Account")
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Switch Account",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = if (activeWorkspace == com.ajyra.amarhishab.model.Workspace.BUSINESS) {
                                    if (isBengali) "ব্যবসায়িক ড্যাশবোর্ড" else "Business Dashboard"
                                } else {
                                    if (isBengali) "ব্যক্তিগত ড্যাশবোর্ড" else "Personal Dashboard"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { dashboardViewModel.refresh() },
                        modifier = Modifier.testTag("dashboard_refresh_button")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = EmeraldPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = if (isBengali) "রিফ্রেশ" else "Refresh",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

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
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { dashboardViewModel.refresh() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("home_screen_scroll"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
            // Hero Balance Card with Balance Hiding
            item {
                HeroBalanceCard(
                    totalBalance = summary.totalBalance,
                    totalIncome = summary.totalIncome,
                    totalExpense = summary.totalExpense,
                    isBengali = isBengali,
                    isBalanceHidden = isBalanceHidden,
                    onToggleHideBalance = { dashboardViewModel.toggleBalanceVisibility() },
                    onIncomeClick = { onNavigateToAddTransaction(false) },
                    onExpenseClick = { onNavigateToAddTransaction(true) },
                    onTransferClick = onNavigateToTransfer
                )
            }

            // Quick Tools Hub (Savings Goals, Statement, Import)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Savings Goals Tool
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .defaultMinSize(minHeight = 92.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onNavigateToSavingsGoals() }
                            .testTag("quick_tool_savings_goals"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(FintechPrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Savings,
                                    contentDescription = null,
                                    tint = FintechPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isBengali) "সঞ্চয় লক্ষ্য" else "Goals",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    // Statement Tool
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .defaultMinSize(minHeight = 92.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onNavigateToStatement() }
                            .testTag("quick_tool_statement"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldPrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isBengali) "স্টেটমেন্ট" else "Statement",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    // Import Tool
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .defaultMinSize(minHeight = 92.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onNavigateToImportData() }
                            .testTag("quick_tool_import"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isBengali) "ইমপোর্ট" else "Import",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Smart Spending Planner Card
            item {
                SmartSpendingCard(
                    summary = smartSpendingSummary,
                    isBengali = isBengali,
                    onViewDetails = onNavigateToSmartSpending
                )
            }

            // Dashboard Active Savings Goal Card
            activeGoal?.let { goal ->
                item {
                    val daysRemaining = goal.getDaysRemaining()
                    val isOverdue = daysRemaining < 0 && !goal.isCompleted
                    val progress = (goal.progressPercentage / 100.0).toFloat()

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onNavigateToSavingsGoals() }
                            .testTag("dashboard_active_savings_goal"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, FintechPrimary.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(FintechPrimary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Savings,
                                            contentDescription = null,
                                            tint = FintechPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = goal.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (goal.isCompleted) {
                                    androidx.compose.material3.Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = IncomeGreen.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (isBengali) "লক্ষ্য অর্জিত" else "Goal Completed",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = IncomeGreen,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                } else {
                                    val percentStr = String.format("%.0f", goal.progressPercentage)
                                    Text(
                                        text = "${if (isBengali) CurrencyFormatter.toBengaliDigits(percentStr) else percentStr}%",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = FintechPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = if (goal.isCompleted) IncomeGreen else FintechPrimary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${CurrencyFormatter.format(goal.savedAmount, isBengali)} / ${CurrencyFormatter.format(goal.targetAmount, isBengali)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (!goal.isCompleted) {
                                    val reqMonthly = goal.getRequiredMonthly(daysRemaining)
                                    Text(
                                        text = if (isOverdue) {
                                            if (isBengali) "তারিখ পার হয়েছে" else "Date passed"
                                        } else {
                                            if (isBengali) "মাসিক: ~${CurrencyFormatter.format(reqMonthly, true)}"
                                            else "Req: ~${CurrencyFormatter.format(reqMonthly, false)}/mo"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOverdue) ExpenseRed else FintechPrimary
                                    )
                                }
                            }
                        }
                    }
                }
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

            // Monthly Comparison Section (This Month vs Previous Month)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_monthly_comparison"),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(FintechPrimary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                                        contentDescription = null,
                                        tint = FintechPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isBengali) "মাসিক তুলনা" else "Monthly Comparison",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isBengali) "চলতি বনাম পূর্ববর্তী মাস" else "This Month vs Previous Month",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (monthlyComparison.thisMonthTxCount > 0 || monthlyComparison.prevMonthTxCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        text = if (isBengali) "${monthlyComparison.thisMonthTxCount} টি লেনদেন" else "${monthlyComparison.thisMonthTxCount} txns",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Income Comparison Row
                        MonthlyComparisonRow(
                            title = if (isBengali) "আয়" else "Income",
                            thisMonthAmount = monthlyComparison.thisMonthIncome,
                            prevMonthAmount = monthlyComparison.prevMonthIncome,
                            changePct = monthlyComparison.incomeChangePct,
                            isIncome = true,
                            isBengali = isBengali
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Expense Comparison Row
                        MonthlyComparisonRow(
                            title = if (isBengali) "ব্যয়" else "Expense",
                            thisMonthAmount = monthlyComparison.thisMonthExpense,
                            prevMonthAmount = monthlyComparison.prevMonthExpense,
                            changePct = monthlyComparison.expenseChangePct,
                            isIncome = false,
                            isBengali = isBengali
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Net Remaining
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isBengali) "অবশিষ্ট সঞ্চয়" else "Net Remaining",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CurrencyFormatter.format(monthlyComparison.thisMonthNet, isBengali),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (monthlyComparison.thisMonthNet >= 0) IncomeGreen else ExpenseRed
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isBengali) "পূর্বের মাস: ${CurrencyFormatter.format(monthlyComparison.prevMonthNet, true)}"
                                    else "Prev Month: ${CurrencyFormatter.format(monthlyComparison.prevMonthNet, false)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val netSign = if (monthlyComparison.netChangePct > 0) "+" else ""
                                val netColor = if (monthlyComparison.thisMonthNet >= monthlyComparison.prevMonthNet) IncomeGreen else ExpenseRed
                                Text(
                                    text = "$netSign${String.format("%.1f", monthlyComparison.netChangePct)}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = netColor
                                )
                            }
                        }
                    }
                }
            }

            // Expense Summary by Category
            if (categoryExpenses.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_expense_summary_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(ExpenseRed.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PieChart,
                                            contentDescription = null,
                                            tint = ExpenseRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (isBengali) "ব্যয় বিবরণী" else "Expense Summary",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = CurrencyFormatter.format(summary.totalExpense, isBengali),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                            }

                            // Show top categories
                            categoryExpenses.take(5).forEach { catExpense ->
                                val percentStr = String.format("%.1f", catExpense.percentage)
                                val displayPercent = if (isBengali) CurrencyFormatter.toBengaliDigits(percentStr) else percentStr
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = catExpense.category,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${CurrencyFormatter.format(catExpense.amount, isBengali)} ($displayPercent%)",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { (catExpense.percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(CircleShape),
                                        color = ExpenseRed,
                                        trackColor = ExpenseRed.copy(alpha = 0.15f)
                                    )
                                }
                            }
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

    if (showWorkspaceSwitchDialog) {
        AlertDialog(
            onDismissRequest = { showWorkspaceSwitchDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBengali) "হিসাবের ধরন পরিবর্তন" else "Switch Workspace",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isBengali)
                            "ব্যক্তিগত ও ব্যবসায়িক হিসাব সম্পূর্ণ আলাদা। প্রতিটি হিসাবের নিজস্ব ব্যালেন্স, রিপোর্ট ও লেনদেন সুরক্ষিত থাকে।"
                        else
                            "Personal and Business accounts remain strictly separate. Each workspace maintains its own isolated balances and records.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Personal Account Card
                    val isPersonalSelected = activeWorkspace == com.ajyra.amarhishab.model.Workspace.PERSONAL
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                dashboardViewModel.switchWorkspace(com.ajyra.amarhishab.model.Workspace.PERSONAL)
                                showWorkspaceSwitchDialog = false
                            }
                            .testTag("switch_to_personal_workspace"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPersonalSelected) EmeraldPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                        ),
                        border = if (isPersonalSelected) BorderStroke(1.5.dp, EmeraldPrimary) else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isPersonalSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (isPersonalSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isBengali) "ব্যক্তিগত হিসাব" else "Personal Account",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPersonalSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isBengali) "দৈনন্দিন খরচ, ব্যক্তিগত সঞ্চয় ও বাজেট" else "Personal expenses, savings & daily budget",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isPersonalSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Active",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // Business Account Card
                    val isBusinessSelected = activeWorkspace == com.ajyra.amarhishab.model.Workspace.BUSINESS
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                dashboardViewModel.switchWorkspace(com.ajyra.amarhishab.model.Workspace.BUSINESS)
                                showWorkspaceSwitchDialog = false
                            }
                            .testTag("switch_to_business_workspace"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isBusinessSelected) EmeraldPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                        ),
                        border = if (isBusinessSelected) BorderStroke(1.5.dp, EmeraldPrimary) else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isBusinessSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = if (isBusinessSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isBengali) "ব্যবসায়িক হিসাব" else "Business Account",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBusinessSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isBengali) "ব্যবসায়ের নগদ প্রবাহ, আয় ও বাণিজ্যিক খরচ" else "Business cashflow, revenues & trade expenses",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isBusinessSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Active",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWorkspaceSwitchDialog = false
                        onNavigateToProfile()
                    }
                ) {
                    Text(
                        text = if (isBengali) "প্রোফাইল পরিচালনা" else "Manage Profile",
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showWorkspaceSwitchDialog = false }) {
                    Text(if (isBengali) "বাতিল" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun MonthlyComparisonRow(
    title: String,
    thisMonthAmount: Double,
    prevMonthAmount: Double,
    changePct: Double,
    isIncome: Boolean,
    isBengali: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${if (isBengali) "চলতি মাস: " else "This Month: "}${CurrencyFormatter.format(thisMonthAmount, isBengali)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isIncome) IncomeGreen else ExpenseRed
            )
            Text(
                text = "${if (isBengali) "পূর্বের মাস: " else "Prev Month: "}${CurrencyFormatter.format(prevMonthAmount, isBengali)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val sign = if (changePct > 0) "+" else ""
        val pctStr = "$sign${String.format("%.1f", changePct)}%"
        val displayPct = if (isBengali) CurrencyFormatter.toBengaliDigits(pctStr) else pctStr

        val badgeColor = if (isIncome) {
            if (changePct >= 0) IncomeGreen else ExpenseRed
        } else {
            if (changePct <= 0) IncomeGreen else ExpenseRed
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = badgeColor.copy(alpha = 0.14f)
        ) {
            Text(
                text = displayPct,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = badgeColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

