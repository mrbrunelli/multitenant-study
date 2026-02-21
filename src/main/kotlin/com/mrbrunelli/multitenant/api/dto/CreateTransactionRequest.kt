package com.mrbrunelli.multitenant.api.dto

import com.mrbrunelli.multitenant.domain.enums.TransactionType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal

data class CreateTransactionRequest(
    @field:NotBlank
    val idempotencyKey: String,
    @field:NotBlank
    val description: String,
    @field:NotNull
    @field:Positive
    val amount: BigDecimal,
    @field:NotNull
    val type: TransactionType,
)
