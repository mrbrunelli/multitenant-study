package com.mrbrunelli.multitenant.api.controller

import com.mrbrunelli.multitenant.MongoIntegrationTest
import com.mrbrunelli.multitenant.domain.document.Transaction
import com.mrbrunelli.multitenant.domain.enums.TransactionType
import com.mrbrunelli.multitenant.domain.repository.TransactionRepository
import com.mrbrunelli.multitenant.infra.tenant.TenantConfig
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionControllerIntegrationTest : MongoIntegrationTest() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var transactionRepository: TransactionRepository

    @BeforeEach
    fun setUp() {
        transactionRepository.deleteAll()
    }

    private fun saveTransaction(tenantId: String, idempotencyKey: String = "key-1"): Transaction =
        transactionRepository.save(
            Transaction(
                tenantId = tenantId,
                idempotencyKey = idempotencyKey,
                description = "Test transaction",
                amount = BigDecimal("100.00"),
                type = TransactionType.INCOME,
            )
        )

    // POST /api/transactions

    @Test
    fun `POST - missing header returns 400`() {
        mockMvc.perform(
            post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"idempotencyKey":"k1","description":"desc","amount":"100","type":"INCOME"}""")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `POST - invalid body missing required field returns 400`() {
        mockMvc.perform(
            post("/api/transactions")
                .header(TenantConfig.HEADER_NAME, "tenant-a")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"idempotencyKey":"k1"}""")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `POST - negative amount returns 400`() {
        mockMvc.perform(
            post("/api/transactions")
                .header(TenantConfig.HEADER_NAME, "tenant-a")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"idempotencyKey":"k1","description":"desc","amount":"-100","type":"INCOME"}""")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `POST - successful creation returns 201 with PENDING status`() {
        mockMvc.perform(
            post("/api/transactions")
                .header(TenantConfig.HEADER_NAME, "tenant-a")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"idempotencyKey":"k1","description":"Test","amount":"100.00","type":"INCOME"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.idempotencyKey").value("k1"))
    }

    @Test
    fun `POST - same idempotency key same tenant returns 200 with same document`() {
        val body = """{"idempotencyKey":"k1","description":"Test","amount":"100.00","type":"INCOME"}"""

        mockMvc.perform(
            post("/api/transactions")
                .header(TenantConfig.HEADER_NAME, "tenant-a")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/api/transactions")
                .header(TenantConfig.HEADER_NAME, "tenant-a")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.idempotencyKey").value("k1"))
    }

    @Test
    fun `POST - same idempotency key different tenant returns 201`() {
        val body = """{"idempotencyKey":"k1","description":"Test","amount":"100.00","type":"INCOME"}"""

        mockMvc.perform(
            post("/api/transactions")
                .header(TenantConfig.HEADER_NAME, "tenant-a")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/api/transactions")
                .header(TenantConfig.HEADER_NAME, "tenant-b")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isCreated)
    }

    // GET /api/transactions

    @Test
    fun `GET - missing header returns 400`() {
        mockMvc.perform(get("/api/transactions"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `GET - returns paginated list for current tenant`() {
        saveTransaction("tenant-a", "key-1")
        saveTransaction("tenant-a", "key-2")

        mockMvc.perform(
            get("/api/transactions")
                .header(TenantConfig.HEADER_NAME, "tenant-a")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.totalElements").value(2))
    }

    @Test
    fun `GET - does not return documents from another tenant`() {
        saveTransaction("tenant-a", "key-1")

        mockMvc.perform(
            get("/api/transactions")
                .header(TenantConfig.HEADER_NAME, "tenant-b")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content.length()").value(0))
    }

    // GET /api/transactions/{id}

    @Test
    fun `GET by ID - missing header returns 400`() {
        mockMvc.perform(get("/api/transactions/some-id"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `GET by ID - existing ID same tenant returns 200`() {
        val transaction = saveTransaction("tenant-a")

        mockMvc.perform(
            get("/api/transactions/${transaction.id}")
                .header(TenantConfig.HEADER_NAME, "tenant-a")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(transaction.id))
    }

    @Test
    fun `GET by ID - non-existing ID returns 404`() {
        mockMvc.perform(
            get("/api/transactions/non-existing-id")
                .header(TenantConfig.HEADER_NAME, "tenant-a")
        ).andExpect(status().isNotFound)
    }

    @Test
    fun `GET by ID - different tenant cannot access document`() {
        val transaction = saveTransaction("tenant-a")

        mockMvc.perform(
            get("/api/transactions/${transaction.id}")
                .header(TenantConfig.HEADER_NAME, "tenant-b")
        ).andExpect(status().isNotFound)
    }

    // PUT /api/transactions/{id}

    @Test
    fun `PUT - missing header returns 400`() {
        mockMvc.perform(
            put("/api/transactions/some-id")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `PUT - successful partial update returns 200 with updated fields`() {
        val transaction = saveTransaction("tenant-a")

        mockMvc.perform(
            put("/api/transactions/${transaction.id}")
                .header(TenantConfig.HEADER_NAME, "tenant-a")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"description":"updated description","status":"COMPLETED"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.description").value("updated description"))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
    }

    @Test
    fun `PUT - non-existing ID returns 404`() {
        mockMvc.perform(
            put("/api/transactions/non-existing-id")
                .header(TenantConfig.HEADER_NAME, "tenant-a")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        ).andExpect(status().isNotFound)
    }

    // DELETE /api/transactions/{id}

    @Test
    fun `DELETE - missing header returns 400`() {
        mockMvc.perform(delete("/api/transactions/some-id"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `DELETE - existing ID returns 204`() {
        val transaction = saveTransaction("tenant-a")

        mockMvc.perform(
            delete("/api/transactions/${transaction.id}")
                .header(TenantConfig.HEADER_NAME, "tenant-a")
        ).andExpect(status().isNoContent)
    }

    @Test
    fun `DELETE - non-existing ID returns 204 (idempotent)`() {
        mockMvc.perform(
            delete("/api/transactions/non-existing-id")
                .header(TenantConfig.HEADER_NAME, "tenant-a")
        ).andExpect(status().isNoContent)
    }
}
