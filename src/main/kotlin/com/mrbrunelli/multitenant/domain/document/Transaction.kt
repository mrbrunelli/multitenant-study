package com.mrbrunelli.multitenant.domain.document

import com.mrbrunelli.multitenant.domain.annotation.TenantId
import com.mrbrunelli.multitenant.domain.enums.TransactionStatus
import com.mrbrunelli.multitenant.domain.enums.TransactionType
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.math.BigDecimal
import java.time.Instant

@Document(collection = "transactions")
@CompoundIndex(def = "{'tenant_id': 1, 'idempotency_key': 1}", unique = true)
data class Transaction(
    @Id
    val id: String? = null,
    @Version
    val version: Long? = null,
    @TenantId
    @Indexed
    @Field("tenant_id")
    val tenantId: String,
    @Field("idempotency_key")
    val idempotencyKey: String,
    val description: String,
    val amount: BigDecimal,
    val type: TransactionType,
    val status: TransactionStatus = TransactionStatus.PENDING,
    @CreatedDate
    val createdAt: Instant? = null,
    @LastModifiedDate
    val updatedAt: Instant? = null,
)
