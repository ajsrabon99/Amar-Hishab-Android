package com.ajyra.amarhishab.presentation.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ajyra.amarhishab.model.AccountType
import com.ajyra.amarhishab.model.Transaction
import com.ajyra.amarhishab.model.TransactionType
import com.ajyra.amarhishab.presentation.components.EmptyStateView
import com.ajyra.amarhishab.presentation.components.TransactionRowItem
import com.ajyra.amarhishab.presentation.viewmodel.SortOption
import com.ajyra.amarhishab.presentation.viewmodel.TransactionsViewModel
import com.ajyra.amarhishab.ui.theme.EmeraldPrimary
import com.ajyra.amarhishab.ui.theme.ExpenseRed
import com.ajyra.amarhishab.ui.theme.IncomeGreen
import com.ajyra.amarhishab.ui.theme.TransferBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    transactionsViewModel: TransactionsViewModel,
    isBengali: Boolean,
    onNavigateToAddTransaction: (isExpense: Boolean) -> Unit
) {
    val transactions by transactionsViewModel.filteredTransactions.collectAsState()
    val filters by transactionsViewModel.filters.collectAsState()
    val actionMessage by transactionsViewModel.actionMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            transactionsViewModel.clearActionMessage()
        }
    }

    if (transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = {
                Text(if (isBengali) "লেনদেন মুছবেন?" else "Delete Transaction?")
            },
            text = {
                val item = transactionToDelete!!
                Text(
                    if (isBengali)
                        "আপনি কি নিশ্চিত যে \"${item.category}\" এর ${item.amount} টাকার লেনদেনটি মুছে ফেলতে চান?"
                    else
                        "Are you sure you want to delete \"${item.category}\" of ${item.amount}?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        transactionToDelete?.let { transactionsViewModel.deleteTransaction(it.id) }
                        transactionToDelete = null
                    }
                ) {
                    Text(if (isBengali) "মুছে ফেলুন" else "Delete", color = ExpenseRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text(if (isBengali) "বাতিল" else "Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isBengali) "লেনদেনসমূহ" else "Transactions",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBengali) "${transactions.size} টি লেনদেন পাওয়া গেছে" else "${transactions.size} records found",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val nextSort = when (filters.sortOption) {
                                SortOption.DATE_DESC -> SortOption.DATE_ASC
                                SortOption.DATE_ASC -> SortOption.AMOUNT_DESC
                                SortOption.AMOUNT_DESC -> SortOption.AMOUNT_ASC
                                else -> SortOption.DATE_DESC
                            }
                            transactionsViewModel.setSortOption(nextSort)
                        },
                        modifier = Modifier.testTag("sort_transactions_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort Transactions",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                onClick = { onNavigateToAddTransaction(true) },
                containerColor = EmeraldPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_transaction")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (isBengali) "লেনদেন যোগ করুন" else "Add Transaction"
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search field
            OutlinedTextField(
                value = filters.searchQuery ?: "",
                onValueChange = { transactionsViewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("transaction_search_input"),
                placeholder = {
                    Text(if (isBengali) "লেনদেন খুঁজুন (খাত, বিবরণ, টাকা)..." else "Search transactions...")
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (!filters.searchQuery.isNullOrEmpty()) {
                        IconButton(onClick = { transactionsViewModel.setSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Type filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filters.type == null,
                    onClick = { transactionsViewModel.setFilterType(null) },
                    label = { Text(if (isBengali) "সব ধরন" else "All Types") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldPrimary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                FilterChip(
                    selected = filters.type == TransactionType.EXPENSE,
                    onClick = {
                        transactionsViewModel.setFilterType(
                            if (filters.type == TransactionType.EXPENSE) null else TransactionType.EXPENSE
                        )
                    },
                    label = { Text(if (isBengali) "ব্যয়" else "Expense") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ExpenseRed,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                FilterChip(
                    selected = filters.type == TransactionType.INCOME,
                    onClick = {
                        transactionsViewModel.setFilterType(
                            if (filters.type == TransactionType.INCOME) null else TransactionType.INCOME
                        )
                    },
                    label = { Text(if (isBengali) "আয়" else "Income") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IncomeGreen,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                FilterChip(
                    selected = filters.type == TransactionType.TRANSFER,
                    onClick = {
                        transactionsViewModel.setFilterType(
                            if (filters.type == TransactionType.TRANSFER) null else TransactionType.TRANSFER
                        )
                    },
                    label = { Text(if (isBengali) "ট্রান্সফার" else "Transfer") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TransferBlue,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }

            // Account filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filters.account == null,
                    onClick = { transactionsViewModel.setFilterAccount(null) },
                    label = { Text(if (isBengali) "সব অ্যাকাউন্ট" else "All Accounts") }
                )
                AccountType.entries.forEach { acc ->
                    FilterChip(
                        selected = filters.account.equals(acc.code, ignoreCase = true),
                        onClick = {
                            transactionsViewModel.setFilterAccount(
                                if (filters.account.equals(acc.code, ignoreCase = true)) null else acc.code
                            )
                        },
                        label = { Text(if (isBengali) acc.displayNameBn else acc.displayNameEn) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Transaction list
            if (transactions.isEmpty()) {
                EmptyStateView(
                    title = if (isBengali) "কোনো লেনদেন মেলেনি" else "No Transactions Match",
                    subtitle = if (isBengali)
                        "ফিল্টার পরিবর্তন করুন অথবা নতুন লেনদেন যুক্ত করুন"
                    else
                        "Try changing the filters or add a new transaction",
                    actionLabel = if (isBengali) "+ লেনদেন যোগ করুন" else "+ Add Transaction",
                    onActionClick = { onNavigateToAddTransaction(true) }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(transactions, key = { it.id }) { transaction ->
                        TransactionRowItem(
                            transaction = transaction,
                            isBengali = isBengali,
                            onClick = { transactionToDelete = transaction }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }
}
