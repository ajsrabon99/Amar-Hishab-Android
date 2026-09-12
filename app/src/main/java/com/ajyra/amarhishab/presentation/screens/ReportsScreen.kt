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
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ajyra.amarhishab.model.AccountType
import com.ajyra.amarhishab.presentation.components.EmptyStateView
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
    val report by reportsViewModel.report.collectAsState()
    val selectedYear by reportsViewModel.selectedYear.collectAsState()
    val selectedMonth by reportsViewModel.selectedMonth.collectAsState()

    val monthName = DateUtils.monthName(selectedMonth, isBengali)
    val yearString = if (isBengali) CurrencyFormatter.toBengaliDigits(selectedYear.toString()) else selectedYear.toString()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isBengali) "মাসিক রিপোর্ট" else "Monthly Reports",
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
            // Month-Year Navigation Selector Card
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
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (selectedMonth > 1) {
                                    reportsViewModel.setSelectedMonth(selectedMonth - 1)
                                } else {
                                    reportsViewModel.setSelectedMonth(12)
                                    reportsViewModel.setSelectedYear(selectedYear - 1)
                                }
                            },
                            modifier = Modifier.testTag("prev_month_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Month"
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$monthName, $yearString",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = {
                                if (selectedMonth < 12) {
                                    reportsViewModel.setSelectedMonth(selectedMonth + 1)
                                } else {
                                    reportsViewModel.setSelectedMonth(1)
                                    reportsViewModel.setSelectedYear(selectedYear + 1)
                                }
                            },
                            modifier = Modifier.testTag("next_month_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Month"
                            )
                        }
                    }
                }
            }

            // Overview Summary Card
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
                            text = if (isBengali) "সারসংক্ষেপ" else "Monthly Overview",
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
                                title = if (isBengali) "সঞ্চয়" else "Net Savings",
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
                        title = if (isBengali) "এই মাসে কোনো তথ্য নেই" else "No Data for This Month",
                        subtitle = if (isBengali)
                            "পূর্বের বা পরের মাস পরীক্ষা করুন অথবা লেনদেন যোগ করুন"
                        else
                            "Check previous/next months or add new transactions"
                    )
                }
            } else {
                // Category breakdown section
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
                                        text = cat.category,
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
                            else -> EmeraldPrimary
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
