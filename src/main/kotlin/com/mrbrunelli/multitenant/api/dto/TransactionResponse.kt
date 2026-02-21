package com.mrbrunelli.multitenant.api.dto

import com.mrbrunelli.multitenant.domain.document.Transaction
import com.mrbrunelli.multitenant.domain.enums.TransactionStatus
import com.mrbrunelli.multitenant.domain.enums.TransactionType
import java.math.BigDecimal
import java.time.Instant

data class TransactionResponse(
    val id: String,
    val version: Long,
    val idempotencyKey: String,
    val description: String,
    val amount: BigDecimal,
    val type: TransactionType,
    val status: TransactionStatus,
    val createdAt: Instant?,
    val updatedAt: Instant?,
) {
    companion object {
        fun from(transaction: Transaction) = TransactionResponse(
            id = transaction.id!!,
            version = transaction.version!!,
            idempotencyKey = transaction.idempotencyKey,
            description = transaction.description,
            amount = transaction.amount,
            type = transaction.type,
            status = transaction.status,
            createdAt = transaction.createdAt,
            updatedAt = transaction.updatedAt,
        )
    }
}
