# Multitenant Spring Boot — Column-based Isolation with MongoDB

A study project exploring **multitenancy** in a Spring Boot 4.x + MongoDB application using a **shared database, shared collection** strategy — where tenant isolation is enforced at the query level through a `tenant_id` column, not through separate databases or collections.

## Multitenancy Strategy

There are three common approaches to multitenancy:

| Strategy | Isolation | Cost | Complexity |
|---|---|---|---|
| Separate database per tenant | Strong | High | High |
| Separate collection per tenant | Medium | Medium | Medium |
| **Shared collection + `tenant_id` column** | Logical | Low | Low |

This project implements the **column-based** approach: all tenants share the same `transactions` collection. Each document carries a `tenant_id` field, and the application layer guarantees that a tenant can only see and modify its own documents.

```
transactions collection
┌─────────────────────────────────────────────────────┐
│ _id  │ tenant_id  │ idempotency_key │ amount │ ...  │
├─────────────────────────────────────────────────────┤
│ aaa  │ tenant-a   │ order-001       │ 100.00 │ ...  │
│ bbb  │ tenant-b   │ order-001       │  50.00 │ ...  │  ← same key, different tenant
│ ccc  │ tenant-a   │ order-002       │  75.00 │ ...  │
└─────────────────────────────────────────────────────┘
```

---

## Architecture

The tenant isolation is implemented across three infrastructure layers that work together transparently, without any coupling to the business logic layer.

```
HTTP Request (X-Tenant-ID: tenant-a)
        │
        ▼
┌──────────────────┐
│ TenantInterceptor│  → reads header, stores in TenantContext, clears after request
└──────────┬───────┘
           │
        ThreadLocal
           │
┌──────────▼───────────────┐
│      TenantContext        │  → holds tenant ID for the duration of the thread
└──────────┬────────────────┘
           │                            ┌────────────────────────────┐
           ├──────── on write ─────────▶│ TenantAwareMongoCallback   │  → fills tenant_id before save
           │                            └────────────────────────────┘
           │
           └──────── on read ──────────▶ TenantAwareMongoTemplate    │  → injects tenant_id filter on all queries
```

---

## Core Components

### `TenantContext`

A thread-safe store for the current tenant ID using a `ThreadLocal`. It is the single source of truth for tenant identity within a request thread.

```kotlin
object TenantContext {
    private val currentTenant = ThreadLocal<String>()

    fun set(tenantId: String) { currentTenant.set(tenantId) }
    fun get(): String = currentTenant.get() ?: throw IllegalStateException("Tenant ID not set in context")
    fun clear() { currentTenant.remove() }
}
```

- `set()` is called by the interceptor at the start of each request.
- `get()` is called by the template and callback to apply isolation.
- `clear()` is called after the request completes to prevent thread pool leaks.

> **Why ThreadLocal?** Each HTTP request is handled by a single thread. `ThreadLocal` allows the tenant ID to be stored once at the entry point (the interceptor) and read anywhere downstream (the template, the callback) without passing it explicitly through every layer.

---

### `TenantInterceptor`

A Spring MVC `HandlerInterceptor` that acts as the entry point for tenant resolution. It runs before every controller method.

```kotlin
override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
    val tenantId = request.getHeader("X-Tenant-ID")

    if (tenantId.isNullOrBlank()) {
        response.status = 400
        response.writer.write("""{"error": "Missing or empty X-Tenant-ID header"}""")
        return false  // request is rejected, controller is never called
    }

    TenantContext.set(tenantId)
    return true
}

override fun afterCompletion(...) {
    TenantContext.clear()  // always runs after the request, even on exceptions
}
```

**Flow:**
1. Request arrives → interceptor reads `X-Tenant-ID` header.
2. If missing → responds with HTTP 400, the controller is never invoked.
3. If present → stores the value in `TenantContext` and lets the request proceed.
4. After the response is sent → `TenantContext.clear()` prevents leaking the tenant ID to the next request on the same thread.

---

### `@TenantId` annotation

A custom field-level annotation used to mark which field of a document holds the tenant identifier.

```kotlin
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class TenantId
```

Applied to the `Transaction` document:

```kotlin
@TenantId
@Field("tenant_id")
val tenantId: String? = null
```

This annotation is the contract between the domain model and the infrastructure layer. The two components below use it to find the right field via reflection — without hardcoding field names.

---

### `TenantAwareMongoCallback`

A Spring Data `BeforeConvertCallback` that automatically fills the `tenant_id` field before a document is saved, so the business logic never has to set it manually.

```kotlin
override fun onBeforeConvert(entity: Any, collection: String): Any {
    val field = entity.javaClass.declaredFields
        .firstOrNull { it.isAnnotationPresent(TenantId::class.java) }
        ?: return entity  // entity has no @TenantId field, skip

    field.isAccessible = true
    val currentValue = field.get(entity) as? String

    if (currentValue.isNullOrBlank()) {
        field.set(entity, TenantContext.get())  // auto-fill from context
    }

    return entity
}
```

**Rules:**
- Entities without `@TenantId` are passed through unmodified.
- If `tenant_id` is already set (e.g., during a test setup), it is **not overwritten** — this prevents accidental re-assignment on updates.
- If `tenant_id` is null or blank, it is filled from `TenantContext`, which holds the value set by the interceptor.

---

### `TenantAwareMongoTemplate`

The most critical piece of the isolation mechanism. It extends Spring's `MongoTemplate` and overrides every read/delete operation to automatically inject a `{ tenant_id: <current_tenant> }` filter into all queries.

```kotlin
override fun <T : Any> findById(id: Any, entityClass: Class<T>, collectionName: String): T? {
    // Instead of: db.transactions.findOne({ _id: id })
    // Executes:   db.transactions.findOne({ _id: id, tenant_id: "tenant-a" })
    val query = Query(Criteria.where("_id").`is`(id).and(fieldName).`is`(tenantId))
    return super.findOne(query, entityClass, collectionName)
}
```

**Overridden operations:** `find`, `findOne`, `findById`, `count`, `exists`, `doRemove`.

**Key behaviors:**
- Uses a `ConcurrentHashMap` cache to resolve the `@TenantId` field name per entity class via reflection — avoiding repeated reflection calls on hot paths.
- If `TenantContext` is not set (e.g., during test setup calling `deleteAll()`), the filter is silently skipped — preventing errors in infrastructure-level operations.
- If the query already contains the `tenant_id` field (e.g., from a derived repository method like `findByIdempotencyKeyAndTenantId`), the filter is not added twice.

---

## Idempotency

The `POST /api/transactions` endpoint supports **tenant-scoped idempotency** via an `idempotency_key` field.

A compound unique index enforces uniqueness per tenant:

```kotlin
@CompoundIndex(def = "{'tenant_id': 1, 'idempotency_key': 1}", unique = true)
```

If the same `idempotency_key` is submitted twice by the **same tenant**, the second request returns the existing document (`HTTP 200`). If submitted by a **different tenant**, it creates a new document (`HTTP 201`) — since the pair `(tenant_id, idempotency_key)` is unique per tenant.

---

## API

All endpoints require the `X-Tenant-ID` header.

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/transactions` | Create a transaction (idempotent) |
| `GET` | `/api/transactions` | List transactions (paginated) |
| `GET` | `/api/transactions/{id}` | Get transaction by ID |
| `PUT` | `/api/transactions/{id}` | Partially update a transaction |
| `DELETE` | `/api/transactions/{id}` | Delete a transaction |

**Request header:**
```
X-Tenant-ID: tenant-a
```

**POST body:**
```json
{
  "idempotencyKey": "order-001",
  "description": "Monthly subscription",
  "amount": 99.90,
  "type": "INCOME"
}
```

---

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Kotlin | 2.2.x | Language |
| Spring Boot | 4.0.x | Framework |
| Spring Data MongoDB | 6.x | Data access |
| MongoDB | 8 | Database |
| Testcontainers | 1.20.4 | Integration tests |
| JUnit 5 | - | Test runner |
| Springdoc OpenAPI | 3.x | API documentation |

---

## Running Locally

**Prerequisites:** Docker

```bash
# Start MongoDB via Docker Compose
docker compose up -d

# Run the application
./gradlew bootRun
```

API docs available at: `http://localhost:8080/swagger-ui.html`

---

## Testing

The test suite is divided into two layers:

### Unit Tests

Test each infrastructure component in isolation, without Spring context:

| Class | What it verifies |
|---|---|
| `TenantContextTest` | ThreadLocal lifecycle: set, get, clear |
| `TenantInterceptorTest` | Missing/blank header → 400; valid header → context set; afterCompletion → context cleared |
| `TenantAwareMongoCallbackTest` | No `@TenantId` field → passthrough; null tenantId → filled from context; existing tenantId → not overwritten |

### Integration Tests

`TransactionControllerIntegrationTest` spins up a real MongoDB instance via Testcontainers and tests all 19 scenarios end-to-end through MockMvc, including:

- Input validation (missing header, invalid body, negative amount)
- Full CRUD lifecycle
- Idempotency: same key + same tenant → `200` (returns existing document)
- **Tenant isolation**: same key + different tenant → `201` (new document created)
- **Tenant isolation on reads**: `GET /id` from a different tenant → `404`
- **Tenant isolation on list**: `GET /` from tenant-b returns empty list when only tenant-a has data

```bash
./gradlew test
```

### Testcontainers Setup

All tests that need a Spring context extend `MongoIntegrationTest`, which holds a single shared `MongoDBContainer` instance and overrides `spring.data.mongodb.uri` via `@DynamicPropertySource`:

```kotlin
abstract class MongoIntegrationTest {
    companion object {
        val mongodb = MongoDBContainer("mongo:7.0").also { it.start() }

        @JvmStatic
        @DynamicPropertySource
        fun mongoProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.mongodb.uri") { "${mongodb.connectionString}/test" }
        }
    }
}
```

> `@DynamicPropertySource` is used instead of `@ServiceConnection` to avoid conflicts with `spring-boot-docker-compose`, which registers its own `MongoConnectionDetails` bean during development — overriding any container-based connection details.
