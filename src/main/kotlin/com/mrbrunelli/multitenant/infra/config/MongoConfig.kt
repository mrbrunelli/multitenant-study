package com.mrbrunelli.multitenant.infra.config

import com.mrbrunelli.multitenant.infra.tenant.TenantAwareMongoTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.convert.MongoConverter

@Configuration
class MongoConfig {

    @Bean
    fun mongoTemplate(mongoDatabaseFactory: MongoDatabaseFactory, mongoConverter: MongoConverter): MongoTemplate =
        TenantAwareMongoTemplate(mongoDatabaseFactory, mongoConverter)
}
