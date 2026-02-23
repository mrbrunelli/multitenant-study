package com.mrbrunelli.multitenant

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MongoDBContainer

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
