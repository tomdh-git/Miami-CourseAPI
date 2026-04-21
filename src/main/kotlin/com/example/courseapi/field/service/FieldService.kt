package com.example.courseapi.field.service

import com.example.courseapi.exceptions.APIException
import com.example.courseapi.field.model.Field
import com.example.courseapi.config.SchoolRegistry
import org.springframework.stereotype.Service

@Service
class FieldService(private val registry: SchoolRegistry) {
    suspend fun getTerms(school: String): List<Field> {
        val connector = registry.getConnector(school)
        val res = connector.getTerms()
        return res.ifEmpty { throw APIException("getTerms returning no terms") }
    }
}