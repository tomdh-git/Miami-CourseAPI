package com.example.courseapi.field

import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.CrossOrigin

import org.springframework.graphql.data.method.annotation.Argument
 

@Controller
@CrossOrigin(origins = ["*"])
class FieldResolver(private val service: FieldService){
    
    @QueryMapping
    suspend fun getTerms(@Argument school: String): FieldResult {
        return runCatching { service.getTerms(school) }
            .fold(
                onSuccess = { SuccessField(it) },
                onFailure = { ErrorField(it.javaClass.simpleName, it.message ?: "Unknown error") }
            )
    }
}