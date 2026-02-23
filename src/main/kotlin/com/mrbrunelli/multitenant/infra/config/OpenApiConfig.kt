package com.mrbrunelli.multitenant.infra.config

import com.mrbrunelli.multitenant.infra.tenant.TenantConfig
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.parameters.Parameter
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun tenantHeaderCustomizer(): OperationCustomizer = OperationCustomizer { operation, _ ->
        operation.addParametersItem(
            Parameter()
                .name(TenantConfig.HEADER_NAME)
                .`in`("header")
                .required(true)
                .schema(StringSchema())
        )
        operation
    }
}
