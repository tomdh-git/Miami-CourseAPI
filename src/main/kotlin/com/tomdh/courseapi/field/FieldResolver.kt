package com.tomdh.courseapi.field

import com.tomdh.courseapi.exceptions.*
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import org.slf4j.LoggerFactory

@DgsComponent
class FieldResolver(private val service: FieldService) {
    private val logger = LoggerFactory.getLogger(FieldResolver::class.java)

    @DgsQuery
    suspend fun getTerms(@InputArgument school: String): FieldResult {
        return resolveQuery(
            logger,
            ::ErrorField
        ) {
            SuccessField(
                service.getTerms(school)
            )
        }
    }
}
