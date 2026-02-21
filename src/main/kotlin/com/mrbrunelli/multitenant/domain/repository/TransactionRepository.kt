package com.mrbrunelli.multitenant.domain.repository

import com.mrbrunelli.multitenant.domain.document.Transaction
import org.springframework.data.mongodb.repository.MongoRepository

interface TransactionRepository : MongoRepository<Transaction, String> {
    fun findByIdempotencyKey(idempotencyKey: String): Transaction?
}
