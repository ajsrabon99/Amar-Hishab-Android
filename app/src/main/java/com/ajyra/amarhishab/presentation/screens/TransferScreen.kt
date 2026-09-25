package com.ajyra.amarhishab.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajyra.amarhishab.model.AccountType
import com.ajyra.amarhishab.presentation.viewmodel.SaveState
import com.ajyra.amarhishab.presentation.viewmodel.TransferViewModel
import com.ajyra.amarhishab.ui.theme.EmeraldPrimary
import com.ajyra.amarhishab.ui.theme.ExpenseRed
import com.ajyra.amarhishab.ui.theme.IncomeGreen
import com.ajyra.amarhishab.ui.theme.TransferBlue
import com.ajyra.amarhishab.utils.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    viewModel: TransferViewModel,
    isBengali: Boolean,
    onNavigateBack: () -> Unit
) {
    val summary by viewModel.summary.collectAsState()

    fun getAccountBalance(acc: AccountType): Double = when (acc) {
        AccountType.CASH -> summary.cashBalance
        AccountType.BKASH -> summary.bkashBalance
        AccountType.NAGAD -> summary.nagadBalance
        AccountType.BANK -> summary.bankBalance
    }

    var fromAccount by remember { mutableStateOf(AccountType.BANK.code) }
    var toAccount by remember { mutableStateOf(AccountType.BKASH.code) }
    var amountText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }

    // Auto-select valid fromAccount if default has 0 balance
    LaunchedEffect(summary) {
        val curBalance = getAccountBalance(AccountType.fromCode(fromAccount))
        if (curBalance <= 0.0) {
            val valid = AccountType.entries.firstOrNull { getAccountBalance(it) > 0.0 }
            if (valid != null) {
                fromAccount = valid.code
            }
        }
    }

    val currentFromBalance = getAccountBalance(AccountType.fromCode(fromAccount))
    val enteredAmount = amountText.toDoubleOrNull()
    val isAmountOver = enteredAmount != null && enteredAmount > currentFromBalance
    val isZeroBalance = currentFromBalance <= 0.0

    val saveState by viewModel.saveState.collectAsState()

    LaunchedEffect(saveState) {
        if (saveState is SaveState.Success) {
            viewModel.resetState()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isBengali) "অ্যাকাউন্ট ট্রান্সফার" else "Account Transfer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("transfer_back_button")) {
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
            // Error display if any
            if (saveState is SaveState.Error) {
                val err = saveState as SaveState.Error
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isBengali) err.messageBn else err.messageEn,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Transfer Direction Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // From Account Label
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBengali) "উৎস অ্যাকাউন্ট (From)" else "From Account",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isBengali) "ব্যালেন্স: ${CurrencyFormatter.format(currentFromBalance, true)}"
                                   else "Balance: ${CurrencyFormatter.format(currentFromBalance, false)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isZeroBalance) ExpenseRed else IncomeGreen
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    // From Account Selection with Zero Balance Visual Indication
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AccountType.entries.forEach { acc ->
                            val balance = getAccountBalance(acc)
                            val isAccZero = balance <= 0.0
                            val isSelected = fromAccount.equals(acc.code, ignoreCase = true)

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when {
                                    isAccZero -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                                    isSelected -> TransferBlue
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    when {
                                        isAccZero -> ExpenseRed.copy(alpha = 0.5f)
                                        isSelected -> TransferBlue
                                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    }
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(enabled = !isAccZero) {
                                        fromAccount = acc.code
                                    }
                                    .testTag("transfer_from_${acc.code}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isAccZero) {
                                            Icon(
                                                imageVector = Icons.Default.Block,
                                                contentDescription = null,
                                                tint = ExpenseRed,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = if (isBengali) acc.displayNameBn else acc.displayNameEn,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                isAccZero -> ExpenseRed
                                                isSelected -> Color.White
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = if (isAccZero) {
                                            if (isBengali) "৳০ (অনুপলব্ধ)" else "৳0 (Unavailable)"
                                        } else {
                                            CurrencyFormatter.format(balance, isBengali)
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when {
                                            isAccZero -> ExpenseRed
                                            isSelected -> Color.White.copy(alpha = 0.85f)
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (isZeroBalance) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isBengali) "সতর্কতা: নির্বাচিত অ্যাকাউন্টে ব্যালেন্স ৳০। অনুগ্রহ করে অন্য সোর্স অ্যাকাউন্ট নির্বাচন করুন।"
                                   else "Notice: Selected account has ৳0 balance. Please choose another source account.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ExpenseRed,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Swap / Arrow divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(TransferBlue.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SyncAlt,
                                contentDescription = null,
                                tint = TransferBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // To Account
                    Text(
                        text = if (isBengali) "গন্তব্য অ্যাকাউন্ট (To)" else "To Account",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
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
                            val isSameAsFrom = fromAccount.equals(acc.code, ignoreCase = true)
                            val balance = getAccountBalance(acc)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (!isSameAsFrom) {
                                        toAccount = acc.code
                                    }
                                },
                                label = {
                                    Column {
                                        Text(if (isBengali) acc.displayNameBn else acc.displayNameEn)
                                        Text(
                                            text = CurrencyFormatter.format(balance, isBengali),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp
                                        )
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = Color.White
                                ),
                                enabled = !isSameAsFrom
                            )
                        }
                    }
                }
            }

            // Amount Input Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isBengali) "ট্রান্সফারের পরিমাণ" else "Transfer Amount",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isBengali) "সর্বোচ্চ: ${CurrencyFormatter.format(currentFromBalance, true)}"
                                   else "Max: ${CurrencyFormatter.format(currentFromBalance, false)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isAmountOver) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transfer_amount_input"),
                        placeholder = { Text("0.00", fontSize = 24.sp) },
                        leadingIcon = {
                            Text(
                                text = "৳",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = TransferBlue,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        },
                        isError = isAmountOver,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (isAmountOver) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isBengali) "অপর্যাপ্ত ব্যালেন্স! আপনার নির্বাচিত অ্যাকাউন্টে আছে ${CurrencyFormatter.format(currentFromBalance, true)}।"
                                   else "Insufficient balance! Available in account: ${CurrencyFormatter.format(currentFromBalance, false)}.",
                            color = ExpenseRed,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Description / Note
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = descriptionText,
                        onValueChange = { descriptionText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transfer_note_input"),
                        label = { Text(if (isBengali) "ট্রান্সফার বিবরণ বা নোট (ঐচ্ছিক)" else "Note (Optional)") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Description, contentDescription = null)
                        },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val isTransferAllowed = !isZeroBalance && !isAmountOver && (enteredAmount ?: 0.0) > 0.0 && !fromAccount.equals(toAccount, ignoreCase = true)

            // Submit Button
            Button(
                onClick = {
                    viewModel.transferMoney(
                        fromAccount = fromAccount,
                        toAccount = toAccount,
                        amountStr = amountText,
                        description = descriptionText.ifBlank { null }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_transfer_button"),
                enabled = isTransferAllowed && saveState !is SaveState.Saving,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TransferBlue,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
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
                        text = if (isBengali) "ট্রান্সফার নিশ্চিত করুন" else "Confirm Transfer",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

