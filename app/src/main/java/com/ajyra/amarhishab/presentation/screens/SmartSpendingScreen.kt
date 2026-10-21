package com.ajyra.amarhishab.presentation.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajyra.amarhishab.data.local.CategoryManager
import com.ajyra.amarhishab.model.DailySpendingStatus
import com.ajyra.amarhishab.presentation.viewmodel.SmartSpendingViewModel
import com.ajyra.amarhishab.ui.theme.EmeraldPrimary
import com.ajyra.amarhishab.ui.theme.ExpenseRed
import com.ajyra.amarhishab.ui.theme.IncomeGreen
import com.ajyra.amarhishab.utils.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartSpendingScreen(
    viewModel: SmartSpendingViewModel,
    isBengali: Boolean,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val categoryManager = remember { CategoryManager.getInstance(context) }
    val summary by viewModel.summary.collectAsState()

    var showEditLimitDialog by remember { mutableStateOf(false) }
    var limitInput by remember { mutableStateOf("") }

    val statusColor = when (summary.todayStatus) {
        DailySpendingStatus.NORMAL -> IncomeGreen
        DailySpendingStatus.ORANGE_WARNING -> Color(0xFFEA580C)
        DailySpendingStatus.RED_ALERT -> ExpenseRed
    }

    val statusTitle = when (summary.todayStatus) {
        DailySpendingStatus.NORMAL -> if (isBengali) "আজকের খরচ সীমার মধ্যে রয়েছে" else "Today's spending is within budget"
        DailySpendingStatus.ORANGE_WARNING -> if (isBengali) "আজকের খরচ কিছুটা বেশি হয়েছে" else "Today's spending is slightly above recommended"
        DailySpendingStatus.RED_ALERT -> if (isBengali) "আজকের খরচ অনেক বেশি হয়েছে!" else "High Spending Alert: Significant Overspending"
    }

    val statusSubtitle = when (summary.todayStatus) {
        DailySpendingStatus.NORMAL -> if (isBengali)
            "আপনার দৈনিক খরচের গতি সন্তোষজনক। এভাবেই বাজেট নিয়ন্ত্রণ বজায় রাখুন।"
        else
            "Your spending pace is under control. Keep it up to protect your monthly savings."
        DailySpendingStatus.ORANGE_WARNING -> if (isBengali)
            "আজ একটু বেশি খরচ হয়েছে। আগামীকাল একটু কম খরচ করলে মাসিক বাজেট ঠিক রাখা যাবে।"
        else
            "Spending is slightly above the daily target. Cut back tomorrow to stay on plan."
        DailySpendingStatus.RED_ALERT -> if (isBengali)
            "আজ আপনার খরচ অনেক বেশি হয়েছে। আগামীকাল কম খরচ করুন যাতে মাসিক বাজেট ঠিক থাকে।"
        else
            "Spending has exceeded the safe daily target. Significantly reduce expenses tomorrow."
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isBengali) "স্মার্ট ব্যয় পরিকল্পনাকারী" else "Smart Spending Planner",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("smart_spending_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            limitInput = if (summary.monthlyLimit % 1.0 == 0.0)
                                summary.monthlyLimit.toLong().toString()
                            else
                                summary.monthlyLimit.toString()
                            showEditLimitDialog = true
                        },
                        modifier = Modifier.testTag("smart_spending_edit_limit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = if (isBengali) "বাজেট পরিবর্তন" else "Edit Limit",
                            tint = EmeraldPrimary
                        )
                    }
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
                .testTag("smart_spending_scroll_view"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // A. Monthly Overview Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isBengali) "মাসিক ব্যয়ের চিত্র" else "Monthly Spending Overview",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isBengali)
                                            "চলতি মাসে বাকি আছে ${CurrencyFormatter.toBengaliDigits(summary.remainingDays.toString())} দিন"
                                        else
                                            "${summary.remainingDays} days remaining this month",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = if (summary.workspace.equals("BUSINESS", true))
                                        (if (isBengali) "ব্যবসা" else "Business")
                                    else
                                        (if (isBengali) "ব্যক্তিগত" else "Personal"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Stats Grid (3 columns)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (isBengali) "নির্ধারিত বাজেট" else "Monthly Limit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = CurrencyFormatter.format(summary.monthlyLimit, isBengali),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isBengali) "মোট ব্যয়" else "Total Spent",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = CurrencyFormatter.format(summary.totalSpent, isBengali),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isBengali) "অবশিষ্ট বাজেট" else "Remaining",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = CurrencyFormatter.format(summary.remainingBudget, isBengali),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (summary.remainingBudget > 0) IncomeGreen else ExpenseRed
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Progress Bar with percentage label
                        val pctFormatted = String.format("%.1f", summary.percentageUsed)
                        val displayPct = if (isBengali) CurrencyFormatter.toBengaliDigits(pctFormatted) else pctFormatted
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isBengali) "বাজেটের ব্যবহার" else "Budget Consumed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$displayPct%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (summary.percentageUsed >= 90) ExpenseRed else EmeraldPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { (summary.percentageUsed / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = when {
                                summary.percentageUsed >= 90.0 -> ExpenseRed
                                summary.percentageUsed >= 75.0 -> Color(0xFFEA580C)
                                else -> EmeraldPrimary
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            // B. Daily Budget & Real-time Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(statusColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Today,
                                        contentDescription = null,
                                        tint = statusColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isBengali) "দৈনিক বাজেট ও গতি" else "Daily Budget & Pace",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isBengali) "আজকের প্রস্তাবিত ব্যয়" else "Calculated for Today",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = statusColor.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = if (isBengali) summary.todayStatus.labelBn else summary.todayStatus.labelEn,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Recommended vs Actual Today
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (isBengali) "প্রস্তাবিত দৈনিক" else "Recommended Today",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = CurrencyFormatter.format(summary.recommendedDailySpending, isBengali),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldPrimary
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isBengali) "আজকের প্রকৃত ব্যয়" else "Actual Spent Today",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = CurrencyFormatter.format(summary.todayActualSpending, isBengali),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = statusColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Professional Alert Message Box
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = statusColor.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.25f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = statusTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = statusSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // C. Data-Driven Financial Insight Card
            summary.financialInsight?.let { insightText ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isBengali) "আর্থিক পর্যবেক্ষণ ও টিপস" else "Financial Observation",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = insightText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // D. Category Breakdown Section
            item {
                Text(
                    text = if (isBengali) "খাতভিত্তিক ব্যয়ের বিভাজন" else "Category Spending Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (summary.categoryBreakdown.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isBengali) "চলতি মাসে কোনো ব্যয়ের তথ্য নেই" else "No expenses recorded this month",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(summary.categoryBreakdown) { catItem ->
                    val displayName = categoryManager.getCategoryDisplayName(catItem.category, isBengali)
                    val pctStr = String.format("%.1f", catItem.percentage)
                    val displayPct = if (isBengali) CurrencyFormatter.toBengaliDigits(pctStr) else pctStr

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${CurrencyFormatter.format(catItem.amount, isBengali)} ($displayPct%)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { (catItem.percentage / 100.0).toFloat().coerceIn(0f, 1f) },
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

            // E. Daily Spending History Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isBengali) "সাম্প্রতিক দিনের ব্যয়ের ইতিহাস" else "Daily Spending History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(summary.dailyHistory) { record ->
                val dayColor = when (record.status) {
                    DailySpendingStatus.NORMAL -> IncomeGreen
                    DailySpendingStatus.ORANGE_WARNING -> Color(0xFFEA580C)
                    DailySpendingStatus.RED_ALERT -> ExpenseRed
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = record.date,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isBengali)
                                    "প্রস্তাবিত: ${CurrencyFormatter.format(record.recommendedAmount, true)}"
                                else
                                    "Budget: ${CurrencyFormatter.format(record.recommendedAmount, false)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = CurrencyFormatter.format(record.actualAmount, isBengali),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = dayColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val diffText = if (record.difference <= 0.0) {
                                val under = -record.difference
                                if (isBengali) "${CurrencyFormatter.format(under, true)} কম" else "${CurrencyFormatter.format(under, false)} under"
                            } else {
                                if (isBengali) "${CurrencyFormatter.format(record.difference, true)} বেশি" else "${CurrencyFormatter.format(record.difference, false)} over"
                            }
                            Text(
                                text = diffText,
                                style = MaterialTheme.typography.labelSmall,
                                color = dayColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Configure Monthly Limit Dialog
    if (showEditLimitDialog) {
        var inputError by remember { mutableStateOf<String?>(null) }
        var recurringEnabled by remember { mutableStateOf(summary.isRecurringLimit) }

        AlertDialog(
            onDismissRequest = { showEditLimitDialog = false },
            title = {
                Text(
                    text = if (isBengali) "মাসিক ব্যয় বাজেট নির্ধারণ" else "Set Monthly Spending Limit",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isBengali)
                            "প্রতি মাসের জন্য আপনার কাঙ্ক্ষিত ব্যয়ের সর্বোচ্চ সীমা নির্ধারণ করুন। সিস্টেম স্বয়ংক্রিয়ভাবে প্রতিদিনের প্রস্তাবিত ব্যয় হিসাব করবে।"
                        else
                            "Specify your monthly spending limit. Amar Hishab will dynamically compute your daily recommended budget.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = limitInput,
                        onValueChange = {
                            limitInput = it
                            inputError = null
                        },
                        label = { Text(if (isBengali) "মাসিক বাজেট (টাকা)" else "Monthly Limit (৳)") },
                        leadingIcon = { Text("৳", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = inputError != null,
                        supportingText = inputError?.let { { Text(it, color = ExpenseRed) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("monthly_limit_input_field"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBengali) "পরবর্তী মাসে স্বয়ংক্রিয়ভাবে বজায় রাখুন" else "Carry forward to next month",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Switch(
                            checked = recurringEnabled,
                            onCheckedChange = { recurringEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = limitInput.toDoubleOrNull()
                        if (num == null || num <= 0.0) {
                            inputError = if (isBengali) "অনুগ্রহ করে শূন্যের বেশি সঠিক পরিমাণ টাকা দিন" else "Please enter a valid amount greater than zero"
                            return@Button
                        }
                        viewModel.setMonthlyLimit(num)
                        viewModel.toggleRecurringLimit(recurringEnabled)
                        showEditLimitDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier.testTag("save_monthly_limit_button")
                ) {
                    Text(if (isBengali) "সংরক্ষণ করুন" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditLimitDialog = false }) {
                    Text(if (isBengali) "বাতিল" else "Cancel")
                }
            }
        )
    }
}
