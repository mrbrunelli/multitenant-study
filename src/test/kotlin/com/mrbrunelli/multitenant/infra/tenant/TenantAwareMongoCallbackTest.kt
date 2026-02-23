package com.mrbrunelli.multitenant.infra.tenant

import com.mrbrunelli.multitenant.domain.annotation.TenantId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class TenantAwareMongoCallbackTest {

    private val callback = TenantAwareMongoCallback()

    data class EntityWithTenantId(@field:TenantId var tenantId: String? = null)
    data class EntityWithoutTenantId(val name: String = "test")

    @BeforeEach
    fun setUp() {
        TenantContext.set("tenant-a")
    }

    @AfterEach
    fun tearDown() {
        TenantContext.clear()
    }

    @Test
    fun `entity without TenantId annotation is returned unmodified`() {
        val entity = EntityWithoutTenantId("test")
        val result = callback.onBeforeConvert(entity, "collection")
        assertSame(entity, result)
    }

    @Test
    fun `entity with null tenantId is filled from TenantContext`() {
        val entity = EntityWithTenantId(tenantId = null)
        callback.onBeforeConvert(entity, "collection")
        assertEquals("tenant-a", entity.tenantId)
    }

    @Test
    fun `entity with existing tenantId is not overwritten`() {
        val entity = EntityWithTenantId(tenantId = "existing-tenant")
        callback.onBeforeConvert(entity, "collection")
        assertEquals("existing-tenant", entity.tenantId)
    }
}
