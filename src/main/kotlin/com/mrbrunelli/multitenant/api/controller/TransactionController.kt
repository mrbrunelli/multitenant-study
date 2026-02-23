package com.mrbrunelli.multitenant.api.controller

import com.mrbrunelli.multitenant.api.dto.CreateTransactionRequest
import com.mrbrunelli.multitenant.api.dto.TransactionResponse
import com.mrbrunelli.multitenant.api.dto.UpdateTransactionRequest
import com.mrbrunelli.multitenant.domain.document.Transaction
import com.mrbrunelli.multitenant.domain.repository.TransactionRepository
import com.mrbrunelli.multitenant.infra.tenant.TenantContext
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/transactions")
class TransactionController(private val transactionRepository: TransactionRepository) {

    @PostMapping
    fun create(@Valid @RequestBody request: CreateTransactionRequest): ResponseEntity<TransactionResponse> {
        val existing = transactionRepository.findByIdempotencyKeyAndTenantId(request.idempotencyKey, TenantContext.get())

        if (existing != null) {
            return ResponseEntity.ok(TransactionResponse.from(existing))
        }

        val transaction = Transaction(
            idempotencyKey = request.idempotencyKey,
            description = request.description,
            amount = request.amount,
            type = request.type,
        )

        val saved = transactionRepository.save(transaction)

        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionResponse.from(saved))
    }

    @GetMapping
    fun listAll(pageable: Pageable): ResponseEntity<Page<TransactionResponse>> {
        val transactions = transactionRepository.findAll(pageable)
        return ResponseEntity.ok(transactions.map(TransactionResponse::from))
    }

    @GetMapping("/{id}")
    fun findById(@PathVariable id: String): ResponseEntity<TransactionResponse> {
        val transaction = transactionRepository.findByIdOrNull(id)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(TransactionResponse.from(transaction))
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: String,
        @RequestBody request: UpdateTransactionRequest,
    ): ResponseEntity<TransactionResponse> {
        val existing = transactionRepository.findByIdOrNull(id)
            ?: return ResponseEntity.notFound().build()

        val updated = existing.copy(
            description = request.description ?: existing.description,
            amount = request.amount ?: existing.amount,
            type = request.type ?: existing.type,
            status = request.status ?: existing.status,
        )

        val saved = transactionRepository.save(updated)
        return ResponseEntity.ok(TransactionResponse.from(saved))
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: String): ResponseEntity<Void> {
        transactionRepository.deleteById(id)
        return ResponseEntity.noContent().build()
    }
}
