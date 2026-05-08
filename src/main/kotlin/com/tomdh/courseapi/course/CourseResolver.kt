package com.tomdh.courseapi.course

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import com.tomdh.courseapi.config.CourseApiProperties
import com.tomdh.courseapi.exceptions.resolveQuery
import com.tomdh.courseapi.generated.types.*
import org.slf4j.LoggerFactory

@DgsComponent
class CourseResolver(
    private val service: CourseService,
    private val properties: CourseApiProperties
) {
    private val logger = LoggerFactory.getLogger(CourseResolver::class.java)

    @DgsQuery
    suspend fun getCourses(
        @InputArgument input: CourseQueryInput,
        @InputArgument limit: Int?
    ): CourseResult {
        @Suppress("UNCHECKED_CAST") //cse271 taught me dis
        val filters = input.filters as Map<String, Any?>

        return resolveQuery(logger, ::ErrorCourse) {
            SuccessCourse(service.getCourses(input.school, filters, limit ?: properties.graphql.defaultResultLimit))
        }
    }
}
