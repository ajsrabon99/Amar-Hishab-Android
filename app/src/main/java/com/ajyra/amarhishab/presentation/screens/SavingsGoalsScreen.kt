package com.ajyra.amarhishab.presentation.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajyra.amarhishab.model.GoalStatus
import com.ajyra.amarhishab.model.SavingsGoal
import com.ajyra.amarhishab.model.SmartSavingsPlan
import com.ajyra.amarhishab.presentation.components.EmptyStateView
import com.ajyra.amarhishab.presentation.viewmodel.SavingsGoalsViewModel
import com.ajyra.amarhishab.ui.theme.EmeraldPrimary
import com.ajyra.amarhishab.ui.theme.ExpenseRed
import com.ajyra.amarhishab.ui.theme.FintechPrimary
import com.ajyra.amarhishab.ui.theme.IncomeGreen
import com.ajyra.amarhishab.utils.CurrencyFormatter
import com.ajyra.amarhishab.utils.DateUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalsScreen(
    viewModel: SavingsGoalsViewModel,
    isBengali: Boolean,
    onNavigateBack: () -> Unit
) {
    val goals by viewModel.goals.collectAsState()
    val activeGoal by viewModel.activeGoal.collectAsState()
    val selectedGoal by viewModel.selectedGoal.collectAsState()
    val smartPlan by viewModel.smartSavingsPlan.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showCreateDialog by remember { mutableStateOf(false) }
    var goalToEdit by remember { mutableStateOf<SavingsGoal?>(null) }
    var goalToDelete by remember { mutableStateOf<SavingsGoal?>(null) }
    var goalToAddMoney by remember { mutableStateOf<SavingsGoal?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isBengali) "সঞ্চয় লক্ষ্য" else "Savings Goals",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBengali) "স্বপ্নের জন্য সঞ্চয় পরিকল্পনা" else "Plan & Track Your Financial Goals",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("savings_goals_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isBengali) "পেছনে যান" else "Back"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.testTag("create_goal_top_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isBengali) "নতুন লক্ষ্য" else "+ Goal",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = FintechPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("create_goal_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBengali) "লক্ষ্য যোগ করুন" else "Create Goal",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (goals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("savings_goals_empty_view")
            ) {
                EmptyStateView(
                    title = if (isBengali) "কোনো সঞ্চয় লক্ষ্য তৈরি করা হয়নি" else "No Savings Goals Yet",
                    subtitle = if (isBengali)
                        "ল্যাপটপ, ফোন, ভ্রমণ বা ইমার্জেন্সি ফান্ডের মতো আর্থিক লক্ষ্য তৈরি করে সঞ্চয় শুরু করুন।"
                    else
                        "Create goals for a laptop, vacation, new phone, or emergency fund to plan your savings.",
                    actionLabel = if (isBengali) "+ নতুন লক্ষ্য তৈরি করুন" else "+ Create First Goal",
                    onActionClick = { showCreateDialog = true }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("savings_goals_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Active Dashboard Goal Section / Banner
                item {
                    val currentActive = activeGoal ?: goals.firstOrNull()
                    if (currentActive != null) {
                        ActiveGoalHighlightCard(
                            goal = currentActive,
                            isBengali = isBengali,
                            onAddMoney = { goalToAddMoney = currentActive },
                            onSelect = { viewModel.selectGoal(currentActive) }
                        )
                    }
                }

                // Smart Savings Suggestions Plan Section
                if (smartPlan != null && smartPlan!!.suggestions.isNotEmpty()) {
                    item {
                        SmartSavingsPlanCard(
                            plan = smartPlan!!,
                            isBengali = isBengali
                        )
                    }
                }

                // Header for All Goals
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBengali) "সকল সঞ্চয় লক্ষ্য (${goals.size})" else "All Goals (${goals.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                // Goal Cards
                items(goals, key = { it.id }) { goal ->
                    val isActive = goal.id == activeGoal?.id
                    SavingsGoalItemCard(
                        goal = goal,
                        isActive = isActive,
                        isBengali = isBengali,
                        onAddMoney = { goalToAddMoney = goal },
                        onEdit = { goalToEdit = goal },
                        onDelete = { goalToDelete = goal },
                        onSetAsActive = { viewModel.setActiveDashboardGoal(goal.id) },
                        onSelectForPlan = { viewModel.selectGoal(goal) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }

    // Create Goal Dialog
    if (showCreateDialog) {
        GoalFormDialog(
            isBengali = isBengali,
            title = if (isBengali) "নতুন সঞ্চয় লক্ষ্য" else "Create Savings Goal",
            confirmButtonLabel = if (isBengali) "সংরক্ষণ করুন" else "Save Goal",
            initialGoal = null,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, amountStr, targetDate, desc ->
                viewModel.createGoal(
                    name = name,
                    targetAmountStr = amountStr,
                    targetDate = targetDate,
                    description = desc
                ) { success, errorBn, errorEn ->
                    if (success) {
                        showCreateDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (isBengali) "সঞ্চয় লক্ষ্য সফলভাবে তৈরি হয়েছে!" else "Savings goal created successfully!"
                            )
                        }
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                (if (isBengali) errorBn else errorEn) ?: "Validation error"
                            )
                        }
                    }
                }
            }
        )
    }

    // Edit Goal Dialog
    goalToEdit?.let { goal ->
        GoalFormDialog(
            isBengali = isBengali,
            title = if (isBengali) "লক্ষ্য সম্পাদনা" else "Edit Savings Goal",
            confirmButtonLabel = if (isBengali) "আপডেট করুন" else "Update Goal",
            initialGoal = goal,
            onDismiss = { goalToEdit = null },
            onConfirm = { name, amountStr, targetDate, desc ->
                viewModel.updateGoal(
                    goal = goal,
                    name = name,
                    targetAmountStr = amountStr,
                    targetDate = targetDate,
                    description = desc
                ) { success, errorBn, errorEn ->
                    if (success) {
                        goalToEdit = null
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (isBengali) "লক্ষ্য সফলভাবে আপডেট হয়েছে!" else "Goal updated successfully!"
                            )
                        }
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                (if (isBengali) errorBn else errorEn) ?: "Validation error"
                            )
                        }
                    }
                }
            }
        )
    }

    // Add Money Dialog
    goalToAddMoney?.let { goal ->
        AddMoneyToGoalDialog(
            goal = goal,
            isBengali = isBengali,
            onDismiss = { goalToAddMoney = null },
            onConfirm = { amountStr ->
                viewModel.addMoneyToGoal(goal.id, amountStr) { success, errorBn, errorEn ->
                    if (success) {
                        goalToAddMoney = null
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (isBengali) "টাকা সফলভাবে জমা হয়েছে!" else "Money saved toward goal!"
                            )
                        }
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                (if (isBengali) errorBn else errorEn) ?: "Error adding money"
                            )
                        }
                    }
                }
            }
        )
    }

    // Delete Confirmation Dialog
    goalToDelete?.let { goal ->
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            title = {
                Text(
                    text = if (isBengali) "লক্ষ্য মুছে ফেলতে চান?" else "Delete Goal?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isBengali)
                        "\"${goal.name}\" সঞ্চয় লক্ষ্যটি মুছে ফেললে এর অগ্রগতি মুছে যাবে। আপনার মূল আয়/ব্যয় লেনদেন অক্ষত থাকবে।"
                    else
                        "Deleting \"${goal.name}\" will remove this goal. Your actual income/expense transactions will remain safe."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGoal(goal.id)
                        goalToDelete = null
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (isBengali) "লক্ষ্য মুছে ফেলা হয়েছে" else "Goal deleted"
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                    modifier = Modifier.testTag("confirm_delete_goal_btn")
                ) {
                    Text(text = if (isBengali) "মুছে ফেলুন" else "Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { goalToDelete = null }) {
                    Text(text = if (isBengali) "বাতিল" else "Cancel")
                }
            }
        )
    }
}

// -------------------------------------------------------------------------
// ACTIVE GOAL HIGHLIGHT CARD
// -------------------------------------------------------------------------

@Composable
private fun ActiveGoalHighlightCard(
    goal: SavingsGoal,
    isBengali: Boolean,
    onAddMoney: () -> Unit,
    onSelect: () -> Unit
) {
    val daysRemaining = goal.getDaysRemaining()
    val isOverdue = daysRemaining < 0 && !goal.isCompleted
    val progress = (goal.progressPercentage / 100.0).toFloat()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("active_savings_goal_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, FintechPrimary.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
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
                            .background(FintechPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = FintechPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = goal.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = FintechPrimary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = if (isBengali) "ড্যাশবোর্ড লক্ষ্য" else "Active Goal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FintechPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = if (goal.isCompleted) {
                                if (isBengali) "🎉 লক্ষ্য সম্পূর্ণ অর্জিত!" else "🎉 Goal Completed!"
                            } else {
                                if (isBengali) "টার্গেট তারিখ: ${DateUtils.formatFriendlyDate(goal.targetDate, true)}"
                                else "Target: ${DateUtils.formatFriendlyDate(goal.targetDate, false)}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (goal.isCompleted) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!goal.isCompleted) {
                    Button(
                        onClick = onAddMoney,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FintechPrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("active_goal_add_money_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = if (isBengali) "জমা" else "Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Amount summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = if (isBengali) "জমা হয়েছে" else "Saved",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.format(goal.savedAmount, isBengali),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (goal.isCompleted) IncomeGreen else FintechPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isBengali) "টার্গেট: ${CurrencyFormatter.format(goal.targetAmount, true)}"
                               else "Target: ${CurrencyFormatter.format(goal.targetAmount, false)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (goal.isCompleted) {
                            if (isBengali) "১০০% অর্জিত" else "100% Completed"
                        } else {
                            val percentStr = String.format("%.1f", goal.progressPercentage)
                            val displayPercent = if (isBengali) CurrencyFormatter.toBengaliDigits(percentStr) else percentStr
                            "$displayPercent%"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (goal.isCompleted) IncomeGreen else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = if (goal.isCompleted) IncomeGreen else FintechPrimary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            if (!goal.isCompleted) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isBengali) "অবশিষ্ট: ${CurrencyFormatter.format(goal.remainingAmount, true)}"
                               else "Remaining: ${CurrencyFormatter.format(goal.remainingAmount, false)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val reqMonthly = goal.getRequiredMonthly(daysRemaining)
                    Text(
                        text = if (isOverdue) {
                            if (isBengali) "তারিখ উত্তীর্ণ" else "Target date passed"
                        } else {
                            if (isBengali) "প্রয়োজন: ~${CurrencyFormatter.format(reqMonthly, true)}/মাস"
                            else "Req: ~${CurrencyFormatter.format(reqMonthly, false)}/mo"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverdue) ExpenseRed else FintechPrimary
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// SMART SAVINGS PLAN CARD
// -------------------------------------------------------------------------

@Composable
private fun SmartSavingsPlanCard(
    plan: SmartSavingsPlan,
    isBengali: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("smart_savings_plan_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(EmeraldPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isBengali) "কীভাবে লক্ষ্য পূরণ করবেন? (স্মার্ট সেভিংস)" else "How to Reach Your Goal (Smart Plan)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isBengali)
                            "${plan.goalName} এর জন্য মাসিক প্রয়োজন ~${CurrencyFormatter.format(plan.requiredMonthly, true)}"
                        else
                            "Monthly needed for ${plan.goalName}: ~${CurrencyFormatter.format(plan.requiredMonthly, false)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isBengali) "ঐচ্ছিক ব্যয় কমিয়ে সম্ভাব্য সঞ্চয়:" else "Suggested savings from discretionary expenses:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            plan.suggestions.forEach { sug ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBengali) sug.reasonBn else sug.reasonEn,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isBengali)
                                    "চলতি মাসে খরচ: ${CurrencyFormatter.format(sug.currentMonthlyExpense, true)}"
                                else
                                    "Spent this month: ${CurrencyFormatter.format(sug.currentMonthlyExpense, false)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = IncomeGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "+${CurrencyFormatter.format(sug.potentialMonthlySavings, isBengali)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Potential Total summary
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = EmeraldPrimary.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isBengali) "মোট সম্ভাব্য অতিরিক্ত সঞ্চয়" else "Total Potential Extra Savings",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "+${CurrencyFormatter.format(plan.totalPotentialMonthlySavings, isBengali)} / মাস",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isBengali) "বাকি মাসিক প্রয়োজন" else "Remaining Monthly Need",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${CurrencyFormatter.format(plan.remainingRequiredMonthly, isBengali)} / মাস",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (plan.remainingRequiredMonthly <= 0.0) IncomeGreen else FintechPrimary
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// INDIVIDUAL SAVINGS GOAL ITEM CARD
// -------------------------------------------------------------------------

@Composable
private fun SavingsGoalItemCard(
    goal: SavingsGoal,
    isActive: Boolean,
    isBengali: Boolean,
    onAddMoney: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetAsActive: () -> Unit,
    onSelectForPlan: () -> Unit
) {
    val daysRemaining = goal.getDaysRemaining()
    val isOverdue = daysRemaining < 0 && !goal.isCompleted
    val progress = (goal.progressPercentage / 100.0).toFloat()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("goal_card_${goal.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (isActive) FintechPrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Goal Name, Active Chip, Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isActive) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = FintechPrimary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = if (isBengali) "সক্রিয়" else "Active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FintechPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (!goal.description.isNullOrBlank()) {
                        Text(
                            text = goal.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = ExpenseRed.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Amount Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = if (isBengali) "জমা হয়েছে" else "Saved",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.format(goal.savedAmount, isBengali),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (goal.isCompleted) IncomeGreen else FintechPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isBengali) "টার্গেট: ${CurrencyFormatter.format(goal.targetAmount, true)}"
                               else "Target: ${CurrencyFormatter.format(goal.targetAmount, false)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (goal.isCompleted) {
                            if (isBengali) "১০০% সম্পন্ন" else "100% Completed"
                        } else {
                            val pStr = String.format("%.1f", goal.progressPercentage)
                            "${if (isBengali) CurrencyFormatter.toBengaliDigits(pStr) else pStr}%"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (goal.isCompleted) IncomeGreen else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (goal.isCompleted) IncomeGreen else FintechPrimary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Target Date & Calculation Breakdown
            if (goal.isCompleted) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = IncomeGreen.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = IncomeGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBengali) "লক্ষ্য অর্জিত হয়েছে! অবশিষ্ট: ৳০" else "Goal Completed! Remaining: ৳0",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                    }
                }
            } else {
                // Time & Required Breakdown
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isBengali) "টার্গেট তারিখ: ${DateUtils.formatFriendlyDate(goal.targetDate, true)}"
                                   else "Target Date: ${DateUtils.formatFriendlyDate(goal.targetDate, false)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isBengali) "অবশিষ্ট: ${CurrencyFormatter.format(goal.remainingAmount, true)}"
                                   else "Remaining: ${CurrencyFormatter.format(goal.remainingAmount, false)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (isOverdue) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isBengali) "টার্গেট তারিখ পার হয়ে গেছে। নতুন তারিখ নির্ধারণ করুন।"
                                       else "Target date has passed. Please update target date.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ExpenseRed,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        // Daily, Weekly, Monthly Breakdown
                        val reqDaily = goal.getRequiredDaily(daysRemaining)
                        val reqWeekly = goal.getRequiredWeekly(daysRemaining)
                        val reqMonthly = goal.getRequiredMonthly(daysRemaining)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isBengali) "দৈনিক: ~${CurrencyFormatter.format(reqDaily, true)}"
                                       else "Daily: ~${CurrencyFormatter.format(reqDaily, false)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isBengali) "সাপ্তাহিক: ~${CurrencyFormatter.format(reqWeekly, true)}"
                                       else "Weekly: ~${CurrencyFormatter.format(reqWeekly, false)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isBengali) "মাসিক: ~${CurrencyFormatter.format(reqMonthly, true)}"
                                       else "Monthly: ~${CurrencyFormatter.format(reqMonthly, false)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = FintechPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!goal.isCompleted) {
                    Button(
                        onClick = onAddMoney,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("goal_add_money_btn_${goal.id}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FintechPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.Paid, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = if (isBengali) "+ টাকা জমা" else "+ Save Money", fontWeight = FontWeight.Bold)
                    }
                }

                if (!isActive) {
                    OutlinedButton(
                        onClick = onSetAsActive,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = if (isBengali) "ড্যাশবোর্ডে রাখুন" else "Show on Dash")
                    }
                }

                OutlinedButton(
                    onClick = onSelectForPlan,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (isBengali) "পরিকল্পনা" else "Plan")
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// ADD MONEY TO GOAL DIALOG
// -------------------------------------------------------------------------

@Composable
private fun AddMoneyToGoalDialog(
    goal: SavingsGoal,
    isBengali: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (amountStr: String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val presetAmounts = listOf("500", "1000", "2000", "5000", "10000")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isBengali) "${goal.name} এ টাকা যোগ করুন" else "Save Money for ${goal.name}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = if (isBengali)
                        "টার্গেট: ${CurrencyFormatter.format(goal.targetAmount, true)} | বর্তমানে আছে: ${CurrencyFormatter.format(goal.savedAmount, true)}"
                    else
                        "Target: ${CurrencyFormatter.format(goal.targetAmount, false)} | Saved: ${CurrencyFormatter.format(goal.savedAmount, false)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        errorMessage = null
                    },
                    label = { Text(if (isBengali) "টাকার পরিমাণ (৳)" else "Amount (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it, color = ExpenseRed) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_money_amount_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presetAmounts) { p ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { amount = p }
                        ) {
                            Text(
                                text = "৳$p",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val num = amount.toDoubleOrNull()
                    if (num == null || num <= 0.0) {
                        errorMessage = if (isBengali) "সঠিক টাকার পরিমাণ দিন" else "Enter a valid amount > 0"
                    } else {
                        onConfirm(amount)
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FintechPrimary),
                modifier = Modifier.testTag("confirm_add_money_btn")
            ) {
                Text(text = if (isBengali) "জমা করুন" else "Save Money", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = if (isBengali) "বাতিল" else "Cancel")
            }
        }
    )
}

// -------------------------------------------------------------------------
// CREATE / EDIT GOAL DIALOG
// -------------------------------------------------------------------------

@Composable
private fun GoalFormDialog(
    isBengali: Boolean,
    title: String,
    confirmButtonLabel: String,
    initialGoal: SavingsGoal?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, amountStr: String, targetDate: String, description: String?) -> Unit
) {
    var name by remember { mutableStateOf(initialGoal?.name ?: "") }
    var targetAmountStr by remember { mutableStateOf(initialGoal?.targetAmount?.let { if (it > 0) it.toLong().toString() else "" } ?: "") }
    var targetDate by remember { mutableStateOf(initialGoal?.targetDate ?: DateUtils.daysFromNow(90)) }
    var description by remember { mutableStateOf(initialGoal?.description ?: "") }

    var validationError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val quickGoals = listOf(
        if (isBengali) "ল্যাপটপ" else "Laptop",
        if (isBengali) "ফোন" else "Phone",
        if (isBengali) "ভ্রমণ" else "Tour",
        if (isBengali) "ক্যামেরা" else "Camera",
        if (isBengali) "ইমার্জেন্সি ফান্ড" else "Emergency Fund",
        if (isBengali) "শিক্ষা" else "Education",
        if (isBengali) "নতুন পিসি" else "New PC"
    )

    fun showDatePicker() {
        val cal = Calendar.getInstance()
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val d = sdf.parse(targetDate)
            if (d != null) cal.time = d
        } catch (_: Exception) {}

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val chosenCal = Calendar.getInstance()
                chosenCal.set(year, month, dayOfMonth)
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                targetDate = sdf.format(chosenCal.time)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis() + (24 * 60 * 60 * 1000)
            show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Quick chips
                if (initialGoal == null) {
                    Text(
                        text = if (isBengali) "জনপ্রিয় লক্ষ্য:" else "Quick Suggestions:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(quickGoals) { q ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { name = q }
                            ) {
                                Text(
                                    text = q,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        validationError = null
                    },
                    label = { Text(if (isBengali) "লক্ষ্যের নাম *" else "Goal Name *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = targetAmountStr,
                    onValueChange = {
                        targetAmountStr = it
                        validationError = null
                    },
                    label = { Text(if (isBengali) "টার্গেট পরিমাণ (৳) *" else "Target Amount (৳) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal_amount_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = targetDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (isBengali) "টার্গেট তারিখ *" else "Target Date *") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker() }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker() }
                        .testTag("goal_date_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(if (isBengali) "বিবরণ (ঐচ্ছিক)" else "Description (Optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal_desc_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        color = ExpenseRed,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        validationError = if (isBengali) "লক্ষ্যের নাম লিখুন।" else "Please enter goal name."
                        return@Button
                    }
                    val amount = targetAmountStr.toDoubleOrNull()
                    if (amount == null || amount <= 0.0) {
                        validationError = if (isBengali) "সঠিক টার্গেট পরিমাণ দিন।" else "Enter a target amount greater than zero."
                        return@Button
                    }
                    val temp = SavingsGoal(id = "", name = name, targetAmount = amount, targetDate = targetDate)
                    if (temp.getDaysRemaining() <= 0) {
                        validationError = if (isBengali) "টার্গেট তারিখ ভবিষ্যতের হতে হবে।" else "Target date must be in the future."
                        return@Button
                    }
                    onConfirm(name.trim(), targetAmountStr.trim(), targetDate, description.ifBlank { null })
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FintechPrimary),
                modifier = Modifier.testTag("save_goal_dialog_btn")
            ) {
                Text(text = confirmButtonLabel, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = if (isBengali) "বাতিল" else "Cancel")
            }
        }
    )
}
