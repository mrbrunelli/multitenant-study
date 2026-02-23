package com.mrbrunelli.multitenant

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.config.EnableMongoAuditing
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

@EnableMongoAuditing
@EnableMongoRepositories(basePackages = ["com.mrbrunelli.multitenant.domain.repository"])
@SpringBootApplication
class MultitenantApplication

fun main(args: Array<String>) {
	runApplication<MultitenantApplication>(*args)
}
