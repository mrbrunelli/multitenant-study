package com.mrbrunelli.multitenant.infra.tenant

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TenantInterceptorTest {

    private val interceptor = TenantInterceptor()

    @AfterEach
    fun tearDown() {
        TenantContext.clear()
    }

    @Test
    fun `preHandle without header returns false and status 400`() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        val result = interceptor.preHandle(request, response, Any())

        assertFalse(result)
        assertEquals(400, response.status)
        assertTrue(response.contentAsString.contains("Missing or empty"))
    }

    @Test
    fun `preHandle with blank header returns false and status 400`() {
        val request = MockHttpServletRequest()
        request.addHeader(TenantConfig.HEADER_NAME, "   ")
        val response = MockHttpServletResponse()

        val result = interceptor.preHandle(request, response, Any())

        assertFalse(result)
        assertEquals(400, response.status)
    }

    @Test
    fun `preHandle with valid header returns true and sets TenantContext`() {
        val request = MockHttpServletRequest()
        request.addHeader(TenantConfig.HEADER_NAME, "tenant-a")
        val response = MockHttpServletResponse()

        val result = interceptor.preHandle(request, response, Any())

        assertTrue(result)
        assertEquals("tenant-a", TenantContext.get())
    }

    @Test
    fun `afterCompletion clears TenantContext`() {
        TenantContext.set("tenant-a")

        interceptor.afterCompletion(MockHttpServletRequest(), MockHttpServletResponse(), Any(), null)

        assertThrows<IllegalStateException> { TenantContext.get() }
    }
}
