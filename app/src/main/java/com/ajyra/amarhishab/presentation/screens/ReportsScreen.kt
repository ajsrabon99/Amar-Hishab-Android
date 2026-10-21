package com.ajyra.amarhishab.presentation.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ajyra.amarhishab.data.local.CategoryManager
import com.ajyra.amarhishab.model.AccountType
import com.ajyra.amarhishab.presentation.components.EmptyStateView
import com.ajyra.amarhishab.presentation.viewmodel.ReportPeriod
import com.ajyra.amarhishab.presentation.viewmodel.ReportsViewModel
import com.ajyra.amarhishab.ui.theme.AccountBkash
import com.ajyra.amarhishab.ui.theme.AccountNagad
import com.ajyra.amarhishab.ui.theme.EmeraldPrimary
import com.ajyra.amarhishab.ui.theme.ExpenseRed
import com.ajyra.amarhishab.ui.theme.IncomeGreen
import com.ajyra.amarhishab.ui.theme.TransferBlue
import com.ajyra.amarhishab.utils.CurrencyFormatter
import com.ajyra.amarhishab.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    reportsViewModel: ReportsViewModel,
    isBengali: Boolean
) {
    val context = LocalContext.current
    val categoryManager = remember { CategoryManager.getInstance(context) }

    val report by reportsViewModel.report.collectAsState()
    val selectedPeriod by reportsViewModel.selectedPeriod.collectAsState()
    val selectedYear by reportsViewModel.selectedYear.collectAsState()
    val selectedMonth by reportsViewModel.selectedMonth.collectAsState()

    val monthName = DateUtils.monthName(selectedMonth, isBengali)
    val yearString = if (isBengali) CurrencyFormatter.toBengaliDigits(selectedYear.toString()) else selectedYear.toString()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isBengali) "আর্থিক রিপোর্ট" else "Financial Reports",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("reports_scroll_view"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Period Filter Section (This Month, Previous Month, This Year)
            item {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reports_period_selector")
                ) {
                    SegmentedButton(
                        selected = selectedPeriod == ReportPeriod.THIS_MONTH,
                        onClick = { reportsViewModel.setSelectedPeriod(ReportPeriod.THIS_MONTH) },
                        shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
                        modifier = Modifier.testTag("period_filter_this_month")
                    ) {
                        Text(
                            text = if (isBengali) "চলতি মাস" else "This Month",
                            fontWeight = if (selectedPeriod == ReportPeriod.THIS_MONTH) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    SegmentedButton(
                        selected = selectedPeriod == ReportPeriod.PREVIOUS_MONTH,
                        onClick = { reportsViewModel.setSelectedPeriod(ReportPeriod.PREVIOUS_MONTH) },
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier.testTag("period_filter_prev_month")
                    ) {
                        Text(
                            text = if (isBengali) "পূর্ববর্তী মাস" else "Previous Month",
                            fontWeight = if (selectedPeriod == ReportPeriod.PREVIOUS_MONTH) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    SegmentedButton(
                        selected = selectedPeriod == ReportPeriod.THIS_YEAR,
                        onClick = { reportsViewModel.setSelectedPeriod(ReportPeriod.THIS_YEAR) },
                        shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
                        modifier = Modifier.testTag("period_filter_this_year")
                    ) {
                        Text(
                            text = if (isBengali) "চলতি বছর" else "This Year",
                            fontWeight = if (selectedPeriod == ReportPeriod.THIS_YEAR) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // 2. Active Period Banner / Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldPrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                val periodHeading = when (selectedPeriod) {
                                    ReportPeriod.THIS_MONTH -> if (isBengali) "চলতি মাস (${monthName})" else "This Month (${monthName})"
                                    ReportPeriod.PREVIOUS_MONTH -> if (isBengali) "পূর্ববর্তী মাস" else "Previous Month"
                                    ReportPeriod.THIS_YEAR -> if (isBengali) "চলতি ক্যালেন্ডার বছর ($yearString)" else "Calendar Year ($yearString)"
                                }
                                Text(
                                    text = periodHeading,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isBengali)
                                        "${if (selectedPeriod == ReportPeriod.THIS_YEAR) yearString else "$monthName, $yearString"} এর হিসাব"
                                    else
                                        "Records for ${report.periodLabel.ifBlank { "Selected Period" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = if (isBengali) "${report.transactionCount} টি লেনদেন" else "${report.transactionCount} txns",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // 3. Overview Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = when (selectedPeriod) {
                                ReportPeriod.THIS_MONTH -> if (isBengali) "চলতি মাসের সারসংক্ষেপ" else "This Month Overview"
                                ReportPeriod.PREVIOUS_MONTH -> if (isBengali) "পূর্ববর্তী মাসের সারসংক্ষেপ" else "Previous Month Overview"
                                ReportPeriod.THIS_YEAR -> if (isBengali) "চলতি বছরের সারসংক্ষেপ" else "This Year Overview"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Income
                            ReportMetricItem(
                                title = if (isBengali) "মোট আয়" else "Total Income",
                                amount = CurrencyFormatter.format(report.income, isBengali),
                                icon = Icons.Default.ArrowDownward,
                                color = IncomeGreen
                            )

                            // Expense
                            ReportMetricItem(
                                title = if (isBengali) "মোট ব্যয়" else "Total Expense",
                                amount = CurrencyFormatter.format(report.expense, isBengali),
                                icon = Icons.Default.ArrowUpward,
                                color = ExpenseRed
                            )

                            // Savings
                            val savingsColor = if (report.saved >= 0) IncomeGreen else ExpenseRed
                            ReportMetricItem(
                                title = if (isBengali) "অবশিষ্ট সঞ্চয়" else "Net Remaining",
                                amount = CurrencyFormatter.format(report.saved, isBengali),
                                icon = Icons.Default.Savings,
                                color = savingsColor
                            )
                        }
                    }
                }
            }

            if (report.income == 0.0 && report.expense == 0.0) {
                item {
                    EmptyStateView(
                        title = if (isBengali) "এই সময়কালে কোনো লেনদেন নেই" else "No Data for Selected Period",
                        subtitle = if (isBengali)
                            "অন্য সময়কাল পরীক্ষা করুন অথবা নতুন লেনদেন যুক্ত করুন"
                        else
                            "Try selecting a different period or record a transaction"
                    )
                }
            } else {
                // Category breakdown section (Expense)
                if (report.categoryBreakdown.isNotEmpty()) {
                    item {
                        Text(
                            text = if (isBengali) "খাতভিত্তিক ব্যয়ের বিশ্লেষণ" else "Expense by Category",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    items(report.categoryBreakdown) { cat ->
                        val catDisplayName = categoryManager.getCategoryDisplayName(cat.category, isBengali)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = catDisplayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val percentStr = String.format("%.1f", cat.percentage)
                                    val displayPercent = if (isBengali) CurrencyFormatter.toBengaliDigits(percentStr) else percentStr
                                    Text(
                                        text = "${CurrencyFormatter.format(cat.amount, isBengali)} ($displayPercent%)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseRed
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { (cat.percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape),
                                    color = ExpenseRed,
                                    trackColor = ExpenseRed.copy(alpha = 0.15f)
                                )
                            }
                        }
                    }
                }

                // Income Source breakdown section
                if (report.incomeBreakdown.isNotEmpty()) {
                    item {
                        Text(
                            text = if (isBengali) "খাতভিত্তিক আয়ের বিশ্লেষণ" else "Income by Source",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    items(report.incomeBreakdown) { inc ->
                        val incDisplayName = categoryManager.getCategoryDisplayName(inc.category, isBengali)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = incDisplayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val percentStr = String.format("%.1f", inc.percentage)
                                    val displayPercent = if (isBengali) CurrencyFormatter.toBengaliDigits(percentStr) else percentStr
                                    Text(
                                        text = "${CurrencyFormatter.format(inc.amount, isBengali)} ($displayPercent%)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = IncomeGreen
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { (inc.percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape),
                                    color = IncomeGreen,
                                    trackColor = IncomeGreen.copy(alpha = 0.15f)
                                )
                            }
                        }
                    }
                }

                // Payment method breakdown section
                if (report.paymentMethodBreakdown.isNotEmpty()) {
                    item {
                        Text(
                            text = if (isBengali) "পেমেন্ট মাধ্যম অনুযায়ী ব্যয়" else "Expense by Payment Method",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    items(report.paymentMethodBreakdown) { accExpense ->
                        val accType = AccountType.fromCode(accExpense.accountCode)
                        val accColor = when (accType) {
                            AccountType.BKASH -> AccountBkash
                            AccountType.NAGAD -> AccountNagad
                            AccountType.BANK -> TransferBlue
                            AccountType.CASH -> IncomeGreen
                        }
                        val accName = if (isBengali) accType.displayNameBn else accType.displayNameEn

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(accColor)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = accName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    val percentStr = String.format("%.1f", accExpense.percentage)
                                    val displayPercent = if (isBengali) CurrencyFormatter.toBengaliDigits(percentStr) else percentStr
                                    Text(
                                        text = "${CurrencyFormatter.format(accExpense.amount, isBengali)} ($displayPercent%)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = accColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { (accExpense.percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape),
                                    color = accColor,
                                    trackColor = accColor.copy(alpha = 0.15f)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ReportMetricItem(
    title: String,
    amount: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(12.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = amount,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
