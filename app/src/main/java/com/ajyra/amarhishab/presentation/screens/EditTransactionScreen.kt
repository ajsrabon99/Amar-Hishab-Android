package com.ajyra.amarhishab.presentation.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.SyncAlt
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
import com.ajyra.amarhishab.data.local.EncryptedSessionManager
import com.ajyra.amarhishab.model.AccountType
import com.ajyra.amarhishab.model.Transaction
import com.ajyra.amarhishab.model.TransactionType
import com.ajyra.amarhishab.presentation.viewmodel.AddTransactionViewModel
import com.ajyra.amarhishab.presentation.viewmodel.SaveState
import com.ajyra.amarhishab.ui.theme.EmeraldPrimary
import com.ajyra.amarhishab.ui.theme.ExpenseRed
import com.ajyra.amarhishab.ui.theme.IncomeGreen
import com.ajyra.amarhishab.ui.theme.TransferBlue
import com.ajyra.amarhishab.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditTransactionScreen(
    transactionId: String,
    viewModel: AddTransactionViewModel,
    isBengali: Boolean,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val categoryManager = remember { CategoryManager.getInstance(context) }
    val sessionManager = remember { EncryptedSessionManager.getInstance(context) }
    val activeWorkspace by sessionManager.currentWorkspace.collectAsState()
    val updateTrigger by categoryManager.categoriesUpdateTrigger.collectAsState()

    var loadedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var currentType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedAccount by remember { mutableStateOf(AccountType.CASH.code) }
    var toAccount by remember { mutableStateOf(AccountType.BKASH.code) }
    var dateText by remember { mutableStateOf(DateUtils.todayDateString()) }
    var descriptionText by remember { mutableStateOf("") }

    val saveState by viewModel.saveState.collectAsState()

    // Fetch existing transaction details
    LaunchedEffect(transactionId) {
        val tx = viewModel.getTransactionById(transactionId)
        if (tx != null) {
            loadedTransaction = tx
            currentType = tx.type
            amountText = if (tx.amount % 1.0 == 0.0) tx.amount.toLong().toString() else tx.amount.toString()
            selectedCategory = tx.category
            selectedAccount = tx.accountCode
            toAccount = tx.toAccountCode ?: AccountType.BKASH.code
            dateText = tx.date
            descriptionText = tx.description ?: ""
        }
        isLoading = false
    }

    LaunchedEffect(saveState) {
        if (saveState is SaveState.Success) {
            viewModel.resetState()
            onNavigateBack()
        }
    }

    val activeCategories = remember(updateTrigger, currentType, activeWorkspace) {
        if (currentType == TransactionType.TRANSFER) emptyList()
        else categoryManager.getAllCategories(currentType, activeWorkspace.id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentType) {
                            TransactionType.EXPENSE -> if (isBengali) "ব্যয় পরিমার্জন" else "Edit Expense"
                            TransactionType.INCOME -> if (isBengali) "আয় পরিমার্জন" else "Edit Income"
                            TransactionType.TRANSFER -> if (isBengali) "ট্রান্সফার পরিমার্জন" else "Edit Transfer"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("edit_transaction_back_button")
                    ) {
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
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = EmeraldPrimary)
            }
        } else if (loadedTransaction == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isBengali) "লেনদেনটি পাওয়া যায়নি" else "Transaction not found",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // If Expense or Income, allow switching between Expense and Income
                if (currentType != TransactionType.TRANSFER) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = currentType == TransactionType.EXPENSE,
                            onClick = {
                                currentType = TransactionType.EXPENSE
                                val newCategories = categoryManager.getAllCategories(TransactionType.EXPENSE, activeWorkspace.id)
                                selectedCategory = newCategories.firstOrNull()?.id ?: "uncategorized"
                            },
                            shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                        ) {
                            Text(
                                text = if (isBengali) "ব্যয়" else "Expense",
                                fontWeight = if (currentType == TransactionType.EXPENSE) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        SegmentedButton(
                            selected = currentType == TransactionType.INCOME,
                            onClick = {
                                currentType = TransactionType.INCOME
                                val newCategories = categoryManager.getAllCategories(TransactionType.INCOME, activeWorkspace.id)
                                selectedCategory = newCategories.firstOrNull()?.id ?: "uncategorized"
                            },
                            shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                        ) {
                            Text(
                                text = if (isBengali) "আয়" else "Income",
                                fontWeight = if (currentType == TransactionType.INCOME) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Error display
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
                                .testTag("edit_transaction_amount_input"),
                            placeholder = { Text("0.00", fontSize = 24.sp) },
                            leadingIcon = {
                                Text(
                                    text = "৳",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (currentType) {
                                        TransactionType.EXPENSE -> ExpenseRed
                                        TransactionType.INCOME -> IncomeGreen
                                        TransactionType.TRANSFER -> TransferBlue
                                    },
                                    modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Category Card (for Expense / Income)
                if (currentType != TransactionType.TRANSFER) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (currentType == TransactionType.EXPENSE) {
                                    if (isBengali) "ব্যয়ের খাত" else "Expense Category"
                                } else {
                                    if (isBengali) "আয়ের খাত" else "Income Source"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                activeCategories.forEach { category ->
                                    val isSelected = selectedCategory.equals(category.id, ignoreCase = true) ||
                                            selectedCategory.equals(category.nameEn, ignoreCase = true)
                                    val categoryName = if (isBengali) category.nameBn else category.nameEn
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedCategory = category.id },
                                        label = { Text(categoryName) },
                                        modifier = Modifier.testTag("edit_category_chip_${category.id}"),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = if (currentType == TransactionType.EXPENSE) ExpenseRed else IncomeGreen,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Accounts Card
                if (currentType == TransactionType.TRANSFER) {
                    // Source Account
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (isBengali) "যে অ্যাকাউন্ট থেকে পাঠানো হচ্ছে (From)" else "From Account",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
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
                                        label = { Text(if (isBengali) acc.displayNameBn else acc.displayNameEn) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = TransferBlue,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = if (isBengali) "যে অ্যাকাউন্টে জমা হবে (To)" else "To Destination Account",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AccountType.entries.forEach { acc ->
                                    val isSelected = toAccount.equals(acc.code, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { toAccount = acc.code },
                                        label = { Text(if (isBengali) acc.displayNameBn else acc.displayNameEn) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = EmeraldPrimary,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Payment Method Card for Expense / Income
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (isBengali) "পেমেন্ট মাধ্যম / অ্যাকাউন্ট" else "Payment Method",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
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
                                        label = { Text(if (isBengali) acc.displayNameBn else acc.displayNameEn) },
                                        modifier = Modifier.testTag("edit_account_chip_${acc.code}"),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = EmeraldPrimary,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Date & Description Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Date Field
                        OutlinedTextField(
                            value = dateText,
                            onValueChange = { dateText = it },
                            label = { Text(if (isBengali) "তারিখ (YYYY-MM-DD)" else "Date (YYYY-MM-DD)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Date",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_transaction_date_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Description Field
                        OutlinedTextField(
                            value = descriptionText,
                            onValueChange = { descriptionText = it },
                            label = { Text(if (isBengali) "বিবরণ বা নোট (ঐচ্ছিক)" else "Description / Note (Optional)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = "Description",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_transaction_description_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Action Buttons
                Button(
                    onClick = {
                        val finalCategory = if (currentType == TransactionType.TRANSFER) "transfer" else selectedCategory
                        viewModel.updateTransaction(
                            id = transactionId,
                            type = currentType,
                            amountStr = amountText,
                            category = finalCategory,
                            accountCode = selectedAccount,
                            toAccountCode = if (currentType == TransactionType.TRANSFER) toAccount else null,
                            dateStr = dateText,
                            description = descriptionText.ifBlank { null }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("edit_transaction_save_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (currentType) {
                            TransactionType.EXPENSE -> ExpenseRed
                            TransactionType.INCOME -> IncomeGreen
                            TransactionType.TRANSFER -> TransferBlue
                        }
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
                            text = if (isBengali) "হালনাগাদ সংরক্ষণ করুন" else "Update Transaction",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
