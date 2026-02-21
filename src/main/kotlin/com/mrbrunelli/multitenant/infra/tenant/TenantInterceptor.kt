package com.mrbrunelli.multitenant.infra.tenant

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class TenantInterceptor : HandlerInterceptor {

    companion object {
        private const val HEADER_NAME = "X-Tenant-ID"
    }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val tenantId = request.getHeader(HEADER_NAME)

        if (tenantId.isNullOrBlank()) {
            response.status = HttpServletResponse.SC_BAD_REQUEST
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.writer.write("""{"error": "Missing or empty $HEADER_NAME header"}""")
            return false
        }

        TenantContext.set(tenantId)
        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?,
    ) {
        TenantContext.clear()
    }
}
