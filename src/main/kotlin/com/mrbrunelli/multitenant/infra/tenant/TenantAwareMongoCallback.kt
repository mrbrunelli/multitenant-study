package com.mrbrunelli.multitenant.infra.tenant

import com.mrbrunelli.multitenant.domain.annotation.TenantId
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertCallback
import org.springframework.stereotype.Component

@Component
class TenantAwareMongoCallback : BeforeConvertCallback<Any> {

    override fun onBeforeConvert(entity: Any, collection: String): Any {
        val field = entity.javaClass.declaredFields.firstOrNull { it.isAnnotationPresent(TenantId::class.java) }
            ?: return entity

        field.isAccessible = true
        val currentValue = field.get(entity) as? String

        if (currentValue.isNullOrBlank()) {
            field.set(entity, TenantContext.get())
        }

        return entity
    }
}
