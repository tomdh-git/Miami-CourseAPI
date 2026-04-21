package com.example.courseapi.config

import org.springframework.stereotype.Component

@Component
class SchoolRegistry(connectors: List<SchoolConnector>) {
    private val map = connectors.associateBy { it.schoolId }

    fun getConnector(schoolId: String): SchoolConnector {
        return map[schoolId] ?: throw IllegalArgumentException("Unknown school ID: $schoolId")
    }

    fun getAllConnectors(): Collection<SchoolConnector> = map.values
}
