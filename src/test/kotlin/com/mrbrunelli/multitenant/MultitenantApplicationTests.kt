package com.mrbrunelli.multitenant

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class MultitenantApplicationTests : MongoIntegrationTest() {

    @Test
    fun contextLoads() {
    }
}
