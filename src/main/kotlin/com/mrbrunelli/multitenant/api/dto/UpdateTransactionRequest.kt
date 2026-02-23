package com.mrbrunelli.multitenant.api.dto

import com.mrbrunelli.multitenant.domain.enums.TransactionStatus
import com.mrbrunelli.multitenant.domain.enums.TransactionType
import java.math.BigDecimal

data class UpdateTransactionRequest(
    val description: String? = null,
    val amount: BigDecimal? = null,
    val type: TransactionType? = null,
    val status: TransactionStatus? = null,
)
