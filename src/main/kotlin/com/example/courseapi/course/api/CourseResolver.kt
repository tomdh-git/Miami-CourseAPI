package com.example.courseapi.course.api

import com.example.courseapi.course.model.CourseResult
import com.example.courseapi.course.model.ErrorCourse
import com.example.courseapi.course.model.SuccessCourse
import com.example.courseapi.course.model.input.CourseByCRNInput
import com.example.courseapi.course.model.input.CourseByInfoInput
import com.example.courseapi.course.service.CourseService
import com.example.courseapi.exceptions.APIException
import com.example.courseapi.exceptions.QueryException
import com.example.courseapi.exceptions.ServerBusyException
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
