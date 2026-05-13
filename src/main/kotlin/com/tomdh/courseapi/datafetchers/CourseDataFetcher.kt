package com.tomdh.courseapi.datafetchers

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import com.tomdh.courseapi.config.CourseApiProperties
import com.tomdh.courseapi.exceptions.resolveQuery
import com.tomdh.courseapi.generated.types.CourseQueryInput
import com.tomdh.courseapi.generated.types.CourseResult
import com.tomdh.courseapi.generated.types.ErrorCourse
import com.tomdh.courseapi.generated.types.SuccessCourse
import com.tomdh.courseapi.service.CourseService
import com.tomdh.courseapi.scalar.JsonScalarUtils
import org.slf4j.LoggerFactory

@DgsComponent
class CourseDataFetcher(
    private val courseService: CourseService,
    private val properties: CourseApiProperties
) {
    private val logger = LoggerFactory.getLogger(CourseDataFetcher::class.java)

    @DgsQuery
    suspend fun getCourses(
        @InputArgument input: CourseQueryInput,
        @InputArgument limit: Int?
    ): CourseResult {
        val filters = JsonScalarUtils.safeCastToMap(input.filters)
        return resolveQuery(logger = logger, errorFactory = ::ErrorCourse) {
            SuccessCourse(
                courses = courseService.getCourses(
                    school = input.school,
                    filters = filters,
                    limit = limit ?: properties.graphql.defaultResultLimit
                )
            )
        }
    }
}
