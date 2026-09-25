package com.ajyra.amarhishab.presentation.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajyra.amarhishab.data.local.CategoryManager
import com.ajyra.amarhishab.model.AccountType
import com.ajyra.amarhishab.model.DefaultCategories
import com.ajyra.amarhishab.model.TransactionType
import com.ajyra.amarhishab.presentation.viewmodel.AddTransactionViewModel
import com.ajyra.amarhishab.presentation.viewmodel.SaveState
import com.ajyra.amarhishab.ui.theme.EmeraldPrimary
import com.ajyra.amarhishab.ui.theme.ExpenseRed
import com.ajyra.amarhishab.ui.theme.IncomeGreen
import com.ajyra.amarhishab.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionScreen(
    viewModel: AddTransactionViewModel,
    initialIsExpense: Boolean = true,
    isBengali: Boolean,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val categoryManager = remember { CategoryManager.getInstance(context) }
    val customCategories by categoryManager.customCategories.collectAsState()

    var isExpense by remember { mutableStateOf(initialIsExpense) }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember {
        mutableStateOf(
            if (initialIsExpense) DefaultCategories.expenseCategories.first().id
            else DefaultCategories.incomeCategories.first().id
        )
    }
    var selectedAccount by remember { mutableStateOf(AccountType.CASH.code) }
    var dateText by remember { mutableStateOf(DateUtils.todayDateString()) }
    var descriptionText by remember { mutableStateOf("") }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    val saveState by viewModel.saveState.collectAsState()

    LaunchedEffect(saveState) {
        if (saveState is SaveState.Success) {
            viewModel.resetState()
            onNavigateBack()
        }
    }

    // Combine default and custom categories for the currently active transaction type
    val currentType = if (isExpense) TransactionType.EXPENSE else TransactionType.INCOME
    val defaultList = if (isExpense) DefaultCategories.expenseCategories else DefaultCategories.incomeCategories
    val customList = customCategories.filter { it.type == currentType }
    val activeCategories = defaultList + customList

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isExpense) {
                            if (isBengali) "ব্যয় যোগ করুন" else "Add Expense"
                        } else {
                            if (isBengali) "আয় যোগ করুন" else "Add Income"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("add_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Segmented switch for Expense vs Income
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = isExpense,
                    onClick = {
                        isExpense = true
                        val expDefault = DefaultCategories.expenseCategories.first().id
                        selectedCategory = expDefault
                    },
                    shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                ) {
                    Text(
                        text = if (isBengali) "ব্যয় (Expense)" else "Expense",
                        fontWeight = if (isExpense) FontWeight.Bold else FontWeight.Normal
                    )
                }
                SegmentedButton(
                    selected = !isExpense,
                    onClick = {
                        isExpense = false
                        val incDefault = DefaultCategories.incomeCategories.first().id
                        selectedCategory = incDefault
                    },
                    shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                ) {
                    Text(
                        text = if (isBengali) "আয় (Income)" else "Income",
                        fontWeight = if (!isExpense) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            // Error display if any
            if (saveState is SaveState.Error) {
                val err = saveState as SaveState.Error
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = if (isBengali) err.messageBn else err.messageEn,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Amount Input Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isBengali) "টাকার পরিমাণ" else "Amount",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transaction_amount_input"),
                        placeholder = { Text("0.00", fontSize = 24.sp) },
                        leadingIcon = {
                            Text(
                                text = "৳",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isExpense) ExpenseRed else IncomeGreen,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Account selection
            Column {
                Text(
                    text = if (isBengali) "অ্যাকাউন্ট / পেমেন্ট মাধ্যম" else "Account / Payment Method",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AccountType.entries.forEach { acc ->
                        val isSelected = selectedAccount.equals(acc.code, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedAccount = acc.code },
                            label = {
                                Text(if (isBengali) acc.displayNameBn else acc.displayNameEn)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Category Selection with "+ Add Custom Category" integrated into the grid/flow
            Column {
                Text(
                    text = if (isBengali) "ক্যাটেগরি / খাত" else "Category",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activeCategories.forEach { cat ->
                        val isSelected = selectedCategory.equals(cat.id, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat.id },
                            label = {
                                Text(if (isBengali) cat.nameBn else cat.nameEn)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (isExpense) ExpenseRed else IncomeGreen,
                                selectedLabelColor = Color.White
                            )
                        )
                    }

                    // Direct "+ Add Custom Category" button at the end of the list/grid
                    SuggestionChip(
                        onClick = { showAddCategoryDialog = true },
                        label = {
                            Text(
                                text = if (isBengali) "নতুন খাত যোগ করুন" else "Add Custom Category",
                                fontWeight = FontWeight.Medium
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            labelColor = MaterialTheme.colorScheme.primary,
                            iconContentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("add_custom_category_chip")
                    )
                }
            }

            // Date & Description
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { dateText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transaction_date_input"),
                        label = { Text(if (isBengali) "তারিখ (YYYY-MM-DD)" else "Date (YYYY-MM-DD)") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = descriptionText,
                        onValueChange = { descriptionText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transaction_desc_input"),
                        label = { Text(if (isBengali) "বিবরণ (ঐচ্ছিক)" else "Description (Optional)") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Description, contentDescription = null)
                        },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Submit Button
            Button(
                onClick = {
                    viewModel.saveTransaction(
                        type = if (isExpense) TransactionType.EXPENSE else TransactionType.INCOME,
                        amountStr = amountText,
                        category = selectedCategory,
                        accountCode = selectedAccount,
                        dateStr = dateText,
                        description = descriptionText.ifBlank { null }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_transaction_button"),
                enabled = saveState !is SaveState.Saving,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isExpense) ExpenseRed else IncomeGreen
                )
            ) {
                if (saveState is SaveState.Saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = if (isBengali) "সংরক্ষণ করুন" else "Save Transaction",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Direct Add Custom Category Dialog
    if (showAddCategoryDialog) {
        var categoryNameInput by remember { mutableStateOf("") }
        var categoryInputError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = {
                Text(
                    text = if (isExpense) {
                        if (isBengali) "নতুন ব্যয় খাত যোগ করুন" else "Add Custom Expense Category"
                    } else {
                        if (isBengali) "নতুন আয় খাত যোগ করুন" else "Add Custom Income Category"
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isBengali)
                            "খাতের নাম লিখুন (যেমন: ${if (isExpense) "Medicine, Gym, Family" else "Freelancing, Tuition, Gift"}):"
                        else
                            "Enter category name (e.g., ${if (isExpense) "Medicine, Gym, Family" else "Freelancing, Tuition, Gift"}):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = categoryNameInput,
                        onValueChange = {
                            categoryNameInput = it
                            categoryInputError = null
                        },
                        label = { Text(if (isBengali) "খাতের নাম" else "Category Name") },
                        placeholder = {
                            Text(if (isExpense) "e.g. Medicine" else "e.g. Freelancing")
                        },
                        singleLine = true,
                        isError = categoryInputError != null,
                        supportingText = categoryInputError?.let { { Text(it, color = ExpenseRed) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_category_name_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = categoryNameInput.trim()
                        if (trimmed.isEmpty()) {
                            categoryInputError = if (isBengali) "খাতের নাম খালি হতে পারে না" else "Category name cannot be empty"
                            return@Button
                        }
                        if (categoryManager.isDuplicateName(trimmed, currentType)) {
                            categoryInputError = if (isBengali) "এই নামের খাত ইতিমধ্যে রয়েছে" else "Category with this name already exists"
                            return@Button
                        }

                        // Add category persistently
                        val newCategory = categoryManager.addCategory(
                            name = trimmed,
                            type = currentType
                        )

                        // Immediately select newly created category and dismiss dialog
                        selectedCategory = newCategory.id
                        showAddCategoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isExpense) ExpenseRed else IncomeGreen
                    ),
                    modifier = Modifier.testTag("confirm_add_category_button")
                ) {
                    Text(if (isBengali) "যুক্ত করুন" else "Add Category")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddCategoryDialog = false },
                    modifier = Modifier.testTag("cancel_add_category_button")
                ) {
                    Text(if (isBengali) "বাতিল" else "Cancel")
                }
            }
        )
    }
}

