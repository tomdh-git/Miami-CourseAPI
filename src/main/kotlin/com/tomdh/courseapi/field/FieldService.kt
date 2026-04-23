package com.tomdh.courseapi.field

import com.tomdh.courseapi.exceptions.types.APIException

@org.springframework.stereotype.Service
class FieldService(private val registry: com.tomdh.courseapi.school.SchoolRegistry) {
    suspend fun getTerms(school: String): List<Field> {
        val connector = registry.getConnector(school)
        return connector.getTerms()
            .ifEmpty { throw APIException("getTerms returning no terms") }
    }
}
