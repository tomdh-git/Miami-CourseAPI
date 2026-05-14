package com.tomdh.courseapi.datafetchers

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import com.tomdh.courseapi.exceptions.resolveQuery
import com.tomdh.courseapi.generated.types.ErrorSchoolSchema
import com.tomdh.courseapi.generated.types.SchoolSchemaResult
import com.tomdh.courseapi.generated.types.SuccessSchoolSchema
import com.tomdh.schoolconnector.school.SchoolRegistry
import org.slf4j.LoggerFactory

@DgsComponent
class SchoolSchemaDataFetcher(private val registry: SchoolRegistry) {
    private val logger = LoggerFactory.getLogger(SchoolSchemaDataFetcher::class.java)

    @DgsQuery
    fun getSchoolSchema(@InputArgument school: String): SchoolSchemaResult =
        resolveQuery(logger = logger, errorFactory = ::ErrorSchoolSchema) {
            val connector = registry.getConnector(school)
            val schema = connector.getSchema()
            SuccessSchoolSchema(
                school = school,
                inputSchema = schema.inputSchema,
                outputSchema = schema.outputSchema
            )
        }
}
