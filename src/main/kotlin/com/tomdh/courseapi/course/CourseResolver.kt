package com.tomdh.courseapi.course

import com.tomdh.courseapi.exceptions.resolveQuery
import com.netflix.graphql.dgs.InputArgument

@com.netflix.graphql.dgs.DgsComponent
class CourseResolver(private val service: CourseService) {
    private val logger = org.slf4j.LoggerFactory.getLogger(CourseResolver::class.java)

    @com.netflix.graphql.dgs.DgsQuery
    suspend fun getCourses(
        @InputArgument input: CourseQueryInput,
        @InputArgument limit: Int?
    ): CourseResult {
        return resolveQuery(logger, ::ErrorCourse) {
            SuccessCourse(service.getCourses(input.school, input.filters, limit ?: 100))
        }
    }
}
