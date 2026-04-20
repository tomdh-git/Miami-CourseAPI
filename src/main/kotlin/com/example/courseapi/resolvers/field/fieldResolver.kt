package com.example.courseapi.resolvers.field

import com.example.courseapi.models.field.FieldResult
import com.example.courseapi.resolvers.utils.field.fieldSafe
import com.example.courseapi.services.field.FieldService
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.CrossOrigin

@Controller
@CrossOrigin(origins = ["*"])
class FieldResolver(private val service: FieldService){
    /**
     * async function getTerms
     * @return FieldResult
     *      FieldSuccess: List<Field>
     *      @throws: FieldError
     *
     * @description
     * calls safe wrapped getTerms in FieldService
     *
     * @example
     * query{getTerms{... on SuccessField{fields{}}... on ErrorField{error, message}}}
     * */
    @QueryMapping
    suspend fun getTerms(): FieldResult = fieldSafe{service.getTerms()}
}