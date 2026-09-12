package com.ajyra.amarhishab.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ajyra.amarhishab.model.SyncStatus
import com.ajyra.amarhishab.model.Transaction
import com.ajyra.amarhishab.model.TransactionType

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    val type: String,
    val amount: Double,
    val category: String,
    val accountCode: String,
    val toAccountCode: String? = null,
    val date: String,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = true,
    val syncStatus: String = "SYNCED"
) {
    fun toDomain(): Transaction = Transaction(
        id = id,
        type = TransactionType.fromString(type),
        amount = amount,
        category = category,
        accountCode = accountCode,
        toAccountCode = toAccountCode,
        date = date,
        description = description,
        createdAt = createdAt,
        isSynced = isSynced,
        syncStatus = try {
            SyncStatus.valueOf(syncStatus)
        } catch (e: Exception) {
            SyncStatus.SYNCED
        }
    )

    companion object {
        fun fromDomain(domain: Transaction): TransactionEntity = TransactionEntity(
            id = domain.id,
            type = domain.type.value,
            amount = domain.amount,
            category = domain.category,
            accountCode = domain.accountCode,
            toAccountCode = domain.toAccountCode,
            date = domain.date,
            description = domain.description,
            createdAt = domain.createdAt,
            isSynced = domain.isSynced,
            syncStatus = domain.syncStatus.name
        )
    }
}
