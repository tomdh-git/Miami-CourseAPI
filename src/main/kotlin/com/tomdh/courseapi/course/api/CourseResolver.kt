package com.tomdh.courseapi.course.api

import com.tomdh.courseapi.course.model.CourseResult
import com.tomdh.courseapi.course.model.ErrorCourse
import com.tomdh.courseapi.course.model.SuccessCourse
import com.tomdh.courseapi.course.model.input.CourseByCRNInput
import com.tomdh.courseapi.course.model.input.CourseByInfoInput
import com.tomdh.courseapi.course.service.CourseService
import com.tomdh.courseapi.exceptions.APIException
import com.tomdh.courseapi.exceptions.QueryException
import com.tomdh.courseapi.exceptions.ServerBusyException
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import org.slf4j.LoggerFactory

@DgsComponent
class CourseResolver(private val service: CourseService) {
    private val logger = LoggerFactory.getLogger(CourseResolver::class.java)

    @DgsQuery
    suspend fun getCourseByInfo(
        @InputArgument input: CourseByInfoInput,
        @InputArgument limit: Int?
    ): CourseResult {
        return runCatching { service.getCourseByInfo(input) }
            .fold(
                onSuccess = { SuccessCourse(it.take(limit ?: 100)) },
                onFailure = { handleError(it) }
            )
    }

    @DgsQuery
    suspend fun getCourseByCRN(@InputArgument input: CourseByCRNInput): CourseResult {
        return runCatching { service.getCourseByCRN(input) }
            .fold(
                onSuccess = { SuccessCourse(it) },
                onFailure = { handleError(it) }
            )
    }

    private fun handleError(e: Throwable): ErrorCourse {
        return when (e) {
            is IllegalArgumentException -> ErrorCourse("VALIDATION_ERROR", e.message ?: "Invalid input")
            is QueryException -> ErrorCourse("QUERY_ERROR", e.message ?: "Query error")
            is APIException -> ErrorCourse("API_ERROR", e.message ?: "Upstream API error")
            is ServerBusyException -> ErrorCourse("SERVER_BUSY", e.message ?: "Server is busy")
            else -> {
                logger.error("Unexpected error in CourseResolver", e)
                ErrorCourse("INTERNAL_ERROR", "An unexpected error occurred")
            }
        }
    }
}
