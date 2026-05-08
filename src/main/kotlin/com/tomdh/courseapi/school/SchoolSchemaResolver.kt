package com.tomdh.courseapi.school

import com.tomdh.courseapi.generated.types.ErrorSchoolSchema
import com.tomdh.courseapi.generated.types.SchoolSchemaResult
import com.tomdh.courseapi.generated.types.SuccessSchoolSchema
import com.tomdh.schoolconnector.school.SchoolRegistry
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import com.tomdh.courseapi.exceptions.resolveQuery
import org.slf4j.LoggerFactory

@DgsComponent
class SchoolSchemaResolver(private val registry: SchoolRegistry) {
    private val logger = LoggerFactory.getLogger(SchoolSchemaResolver::class.java)

    @DgsQuery
    fun getSchoolSchema(@InputArgument school: String): SchoolSchemaResult {
        return resolveQuery(logger, ::ErrorSchoolSchema) {
            val connector = registry.getConnector(school)
            val schema = connector.getSchema()
            SuccessSchoolSchema(
                school = school,
                inputSchema = schema.inputSchema as Object,
                outputSchema = schema.outputSchema as Object
            )
        }
    }
}
