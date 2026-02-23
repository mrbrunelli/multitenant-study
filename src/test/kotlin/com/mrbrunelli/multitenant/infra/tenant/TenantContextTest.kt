package com.mrbrunelli.multitenant.infra.tenant

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class TenantContextTest {

    @AfterEach
    fun tearDown() {
        TenantContext.clear()
    }

    @Test
    fun `get without set throws IllegalStateException`() {
        assertThrows<IllegalStateException> { TenantContext.get() }
    }

    @Test
    fun `set and get returns the value`() {
        TenantContext.set("tenant-a")
        assertEquals("tenant-a", TenantContext.get())
    }

    @Test
    fun `clear makes get throw again`() {
        TenantContext.set("tenant-a")
        TenantContext.clear()
        assertThrows<IllegalStateException> { TenantContext.get() }
    }
}
