package com.tomdh.courseapi.course

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import com.tomdh.courseapi.exceptions.resolveQuery
import org.slf4j.LoggerFactory

@DgsComponent
class CourseResolver(private val service: CourseService) {
    private val logger = LoggerFactory.getLogger(CourseResolver::class.java)

    @DgsQuery
    suspend fun getCourses(
        @InputArgument input: CourseQueryInput,
        @InputArgument limit: Int?
    ): CourseResult {
        return resolveQuery(logger, ::ErrorCourse) {
            SuccessCourse(service.getCourses(input.school, input.filters, limit ?: 100))
        }
    }
}
