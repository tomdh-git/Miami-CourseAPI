package com.tomdh.courseapi.field.service

import com.tomdh.courseapi.exceptions.APIException
import com.tomdh.courseapi.field.model.Field
import com.tomdh.courseapi.school.SchoolRegistry
import org.springframework.stereotype.Service

@Service
class FieldService(private val registry: SchoolRegistry) {
    suspend fun getTerms(school: String): List<Field> {
        val connector = registry.getConnector(school)
        val res = connector.getTerms()
        return res.ifEmpty { throw APIException("getTerms returning no terms") }
    }
}