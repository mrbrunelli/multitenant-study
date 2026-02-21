package com.mrbrunelli.multitenant

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.config.EnableMongoAuditing

@EnableMongoAuditing
@SpringBootApplication
class MultitenantApplication

fun main(args: Array<String>) {
	runApplication<MultitenantApplication>(*args)
}
