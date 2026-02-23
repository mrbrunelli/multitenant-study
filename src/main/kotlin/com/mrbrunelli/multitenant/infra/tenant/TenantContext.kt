package com.mrbrunelli.multitenant.infra.tenant

object TenantContext {

    private val currentTenant = ThreadLocal<String>()

    fun set(tenantId: String) {
        currentTenant.set(tenantId)
    }

    fun get(): String =
        currentTenant.get() ?: throw IllegalStateException("Tenant ID not set in context")

    fun clear() {
        currentTenant.remove()
    }
}
