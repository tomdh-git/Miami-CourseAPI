package com.tomdh.courseapi.datafetchers

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import com.tomdh.courseapi.exceptions.resolveQuery
import com.tomdh.courseapi.generated.types.ErrorField
import com.tomdh.courseapi.generated.types.FieldResult
import com.tomdh.courseapi.generated.types.SuccessField
import com.tomdh.courseapi.service.FieldService
import org.slf4j.LoggerFactory

@DgsComponent
class FieldDataFetcher(private val fieldService: FieldService) {
    private val logger = LoggerFactory.getLogger(FieldDataFetcher::class.java)

    @DgsQuery
    suspend fun getTerms(@InputArgument school: String): FieldResult =
        resolveQuery(logger = logger, errorFactory = ::ErrorField) {
            SuccessField(fields = fieldService.getTerms(school))
        }
}
