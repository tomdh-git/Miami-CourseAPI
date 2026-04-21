package com.example.courseapi.field.api

import com.example.courseapi.exceptions.APIException
import com.example.courseapi.field.model.ErrorField
import com.example.courseapi.field.model.FieldResult
import com.example.courseapi.field.model.SuccessField
import com.example.courseapi.field.service.FieldService
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import org.slf4j.LoggerFactory

@DgsComponent
class FieldResolver(private val service: FieldService) {
    private val logger = LoggerFactory.getLogger(FieldResolver::class.java)

    @DgsQuery
    suspend fun getTerms(@InputArgument school: String): FieldResult {
        return runCatching { service.getTerms(school) }
            .fold(
                onSuccess = { SuccessField(it) },
                onFailure = { e ->
                    when (e) {
                        is IllegalArgumentException -> ErrorField("VALIDATION_ERROR", e.message ?: "Invalid input")
                        is APIException -> ErrorField("API_ERROR", e.message ?: "Upstream API error")
                        else -> {
                            logger.error("Unexpected error in FieldResolver", e)
                            ErrorField("INTERNAL_ERROR", "An unexpected error occurred")
                        }
                    }
                }
            )
    }
}