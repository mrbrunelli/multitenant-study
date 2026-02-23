package com.mrbrunelli.multitenant.infra.config

import com.mrbrunelli.multitenant.infra.tenant.TenantAwareMongoTemplate
import com.mongodb.ConnectionString
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.convert.MappingMongoConverter

@Configuration
class MongoConfig : AbstractMongoClientConfiguration() {

    @Value($$"${spring.data.mongodb.uri}")
    private lateinit var mongoUri: String

    override fun getDatabaseName(): String =
        ConnectionString(mongoUri).database ?: error("Database name missing from URI")

    override fun mongoClient(): MongoClient = MongoClients.create(mongoUri)

    @Bean
    override fun mongoTemplate(databaseFactory: MongoDatabaseFactory, converter: MappingMongoConverter): MongoTemplate =
        TenantAwareMongoTemplate(databaseFactory, converter)
}
