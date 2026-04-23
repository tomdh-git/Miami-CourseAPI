package com.tomdh.courseapi.field

import com.tomdh.courseapi.exceptions.resolveQuery
import com.netflix.graphql.dgs.InputArgument

@com.netflix.graphql.dgs.DgsComponent
class FieldResolver(private val service: FieldService) {
    private val logger = org.slf4j.LoggerFactory.getLogger(FieldResolver::class.java)

    @com.netflix.graphql.dgs.DgsQuery
    suspend fun getTerms(@InputArgument school: String): FieldResult {
        return resolveQuery(logger, ::ErrorField) {
            SuccessField(service.getTerms(school))
        }
    }
}
