package com.tomdh.courseapi.school

import com.tomdh.courseapi.exceptions.toErrorResponse
import com.netflix.graphql.dgs.InputArgument

@com.netflix.graphql.dgs.DgsComponent
class SchoolSchemaResolver(private val registry: SchoolRegistry) {
    private val logger = org.slf4j.LoggerFactory.getLogger(SchoolSchemaResolver::class.java)

    @com.netflix.graphql.dgs.DgsQuery
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
