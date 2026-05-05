package com.tomdh.courseapi.school

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import com.tomdh.courseapi.exceptions.toErrorResponse
import org.slf4j.LoggerFactory

@DgsComponent
class SchoolSchemaResolver(private val registry: SchoolRegistry) {
    private val logger = LoggerFactory.getLogger(SchoolSchemaResolver::class.java)

    @DgsQuery
    fun getSchoolSchema(@InputArgument school: String): SchoolSchemaResult {
        return try {
            val connector = registry.getConnector(school)
            val schema = connector.getSchema()
            SuccessSchoolSchema(
                school = school,
                inputSchema = schema.inputSchema,
                outputSchema = schema.outputSchema
            )
        } catch (e: Exception) {
            val (code, msg) = e.toErrorResponse(logger)
            ErrorSchoolSchema(code, msg)
        }
    }
}
