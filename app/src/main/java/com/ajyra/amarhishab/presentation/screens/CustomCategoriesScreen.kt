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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajyra.amarhishab.data.local.CategoryManager
import com.ajyra.amarhishab.model.CategoryItem
import com.ajyra.amarhishab.model.TransactionType
import com.ajyra.amarhishab.ui.theme.EmeraldPrimary
import com.ajyra.amarhishab.ui.theme.ExpenseRed
import com.ajyra.amarhishab.ui.theme.IncomeGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomCategoriesScreen(
    isBengali: Boolean,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val categoryManager = remember { CategoryManager.getInstance(context) }
    val customCategories by categoryManager.customCategories.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTabIsExpense by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<CategoryItem?>(null) }
    var categoryToDelete by remember { mutableStateOf<CategoryItem?>(null) }

    val currentType = if (selectedTabIsExpense) TransactionType.EXPENSE else TransactionType.INCOME
    val displayedCustomCategories = customCategories.filter { it.type == currentType }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isBengali) "কাস্টম ক্যাটাগরি" else "Custom Categories",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_custom_category_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Category")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Income / Expense selector
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedTabIsExpense,
                    onClick = { selectedTabIsExpense = true },
                    shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                ) {
                    Text(if (isBengali) "ব্যয় খাত" else "Expense Categories")
                }
                SegmentedButton(
                    selected = !selectedTabIsExpense,
                    onClick = { selectedTabIsExpense = false },
                    shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                ) {
                    Text(if (isBengali) "আয় খাত" else "Income Categories")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (displayedCustomCategories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = if (isBengali) "কোনো কাস্টম খাত যোগ করা হয়নি" else "No custom categories added yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isBengali) "নিচের '+' বাটনে চাপ দিয়ে আপনার নিজস্ব খাত যোগ করুন" else "Tap '+' button below to create your own category",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(displayedCustomCategories, key = { it.id }) { cat ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("category_card_${cat.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (cat.type == TransactionType.EXPENSE) ExpenseRed.copy(alpha = 0.15f)
                                            else IncomeGreen.copy(alpha = 0.15f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Category,
                                        contentDescription = null,
                                        tint = if (cat.type == TransactionType.EXPENSE) ExpenseRed else IncomeGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isBengali) cat.nameBn else cat.nameEn,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (isBengali) "কাস্টম তৈরি খাত" else "Custom Category",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(onClick = { categoryToEdit = cat }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(onClick = { categoryToDelete = cat }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = ExpenseRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Category Dialog
    if (showAddDialog) {
        var newCatName by remember { mutableStateOf("") }
        var isExpenseType by remember { mutableStateOf(selectedTabIsExpense) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = if (isBengali) "নতুন খাত যুক্ত করুন" else "Add Own Category",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = isExpenseType,
                            onClick = {
                                isExpenseType = true
                                errorMessage = null
                            },
                            shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                        ) {
                            Text(if (isBengali) "ব্যয়" else "Expense")
                        }
                        SegmentedButton(
                            selected = !isExpenseType,
                            onClick = {
                                isExpenseType = false
                                errorMessage = null
                            },
                            shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                        ) {
                            Text(if (isBengali) "আয়" else "Income")
                        }
                    }

                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = {
                            newCatName = it
                            errorMessage = null
                        },
                        label = { Text(if (isBengali) "খাতের নাম লিখুন" else "Category Name") },
                        singleLine = true,
                        isError = errorMessage != null,
                        supportingText = errorMessage?.let { { Text(it, color = ExpenseRed) } },
                        modifier = Modifier.fillMaxWidth().testTag("add_category_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newCatName.trim()
                        if (trimmed.isEmpty()) {
                            errorMessage = if (isBengali) "খাতের নাম খালি হতে পারে না" else "Category name cannot be empty"
                            return@Button
                        }
                        val type = if (isExpenseType) TransactionType.EXPENSE else TransactionType.INCOME
                        if (categoryManager.isDuplicateName(trimmed, type)) {
                            errorMessage = if (isBengali) "এই নামের খাত ইতিমধ্যে রয়েছে" else "Category already exists"
                            return@Button
                        }
                        categoryManager.addCategory(trimmed, type)
                        showAddDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (isBengali) "'$trimmed' খাত সফলভাবে যুক্ত হয়েছে" else "'$trimmed' category added successfully"
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(if (isBengali) "সংরক্ষণ" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(if (isBengali) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Edit Category Dialog
    categoryToEdit?.let { cat ->
        var editName by remember { mutableStateOf(cat.nameEn) }
        var editError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { categoryToEdit = null },
            title = {
                Text(
                    text = if (isBengali) "খাত সম্পাদনা করুন" else "Edit Category",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = {
                            editName = it
                            editError = null
                        },
                        label = { Text(if (isBengali) "খাতের নাম" else "Category Name") },
                        singleLine = true,
                        isError = editError != null,
                        supportingText = editError?.let { { Text(it, color = ExpenseRed) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = editName.trim()
                        if (trimmed.isEmpty()) {
                            editError = if (isBengali) "খাতের নাম খালি হতে পারে না" else "Category name cannot be empty"
                            return@Button
                        }
                        val success = categoryManager.editCategory(cat.id, trimmed)
                        if (!success) {
                            editError = if (isBengali) "একই নামের খাত ইতিমধ্যে রয়েছে" else "Category with this name already exists"
                        } else {
                            categoryToEdit = null
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (isBengali) "খাত হালনাগাদ করা হয়েছে" else "Category updated"
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(if (isBengali) "সংরক্ষণ" else "Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToEdit = null }) {
                    Text(if (isBengali) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    categoryToDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = {
                Text(
                    text = if (isBengali) "খাতটি মুছে ফেলতে চান?" else "Delete Category?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isBengali)
                        "'${cat.nameBn}' খাতটি তালিকা থেকে মুছে ফেলা হবে। পূর্বের সংরক্ষিত লেনদেনসমূহের তথ্য অক্ষুণ্ণ থাকবে।"
                    else
                        "Category '${cat.nameEn}' will be removed from future selection. Historical transactions using this category will remain safe."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        categoryManager.deleteCategory(cat.id)
                        categoryToDelete = null
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (isBengali) "খাত মুছে ফেলা হয়েছে" else "Category deleted"
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text(if (isBengali) "মুছে ফেলুন" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text(if (isBengali) "বাতিল" else "Cancel")
                }
            }
        )
    }
}
