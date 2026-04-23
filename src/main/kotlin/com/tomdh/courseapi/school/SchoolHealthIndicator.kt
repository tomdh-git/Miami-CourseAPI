package com.tomdh.courseapi.school

import kotlinx.coroutines.runBlocking
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator

@org.springframework.stereotype.Component
class SchoolHealthIndicator(private val registry: SchoolRegistry) : HealthIndicator {

    override fun health(): Health {
        val builder = Health.up()
        val details = mutableMapOf<String, Any>()

        for (connector in registry.getAllConnectors()) {
            val available = try {
                runBlocking { connector.isAvailable() }
            } catch (_: Exception) {
                false
            }
            details[connector.schoolId] = if (available) "UP" else "DOWN"
            if (!available) {
                builder.down()
            }
        }

        return builder
            .withDetails(details)
            .build()
    }
}