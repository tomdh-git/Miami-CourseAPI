package com.tomdh.courseapi.field

import com.tomdh.schoolconnector.exceptions.types.APIException
import com.tomdh.schoolconnector.school.SchoolRegistry
import com.tomdh.schoolconnector.field.Field
import org.springframework.stereotype.Service

@Service
class FieldService(private val registry: SchoolRegistry) {
    suspend fun getTerms(school: String): List<Field> {
        val connector = registry.getConnector(school)
        return connector.getTerms()
            .ifEmpty { throw APIException("getTerms returning no terms") }
    }
}
