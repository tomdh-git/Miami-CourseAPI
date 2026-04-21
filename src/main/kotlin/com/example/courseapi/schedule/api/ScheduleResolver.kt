package com.example.courseapi.schedule.api

import com.example.courseapi.exceptions.APIException
import com.example.courseapi.exceptions.QueryException
import com.example.courseapi.exceptions.ServerBusyException
import com.example.courseapi.schedule.model.ErrorSchedule
import com.example.courseapi.schedule.model.ScheduleResult
import com.example.courseapi.schedule.model.SuccessSchedule
import com.example.courseapi.schedule.model.input.FillerByAttributesInput
import com.example.courseapi.schedule.model.input.ScheduleByCourseInput
import com.example.courseapi.schedule.service.ScheduleService
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import org.slf4j.LoggerFactory

@DgsComponent
class ScheduleResolver(private val scheduleService: ScheduleService) {
    private val logger = LoggerFactory.getLogger(ScheduleResolver::class.java)

    @DgsQuery
    suspend fun getScheduleByCourses(
        @InputArgument input: ScheduleByCourseInput,
        @InputArgument limit: Int?
    ): ScheduleResult {
        return runCatching { scheduleService.getScheduleByCourses(input) }
            .fold(
                onSuccess = { SuccessSchedule(it.take(limit ?: 100)) },
                onFailure = { handleError(it) }
            )
    }

    @DgsQuery
    suspend fun getFillerByAttributes(
        @InputArgument input: FillerByAttributesInput,
        @InputArgument limit: Int?
    ): ScheduleResult {
        return runCatching { scheduleService.getFillerByAttributes(input) }
            .fold(
                onSuccess = { SuccessSchedule(it.take(limit ?: 100)) },
                onFailure = { handleError(it) }
            )
    }

    private fun handleError(e: Throwable): ErrorSchedule {
        return when (e) {
            is IllegalArgumentException -> ErrorSchedule("VALIDATION_ERROR", e.message ?: "Invalid input")
            is QueryException -> ErrorSchedule("QUERY_ERROR", e.message ?: "Query error")
            is APIException -> ErrorSchedule("API_ERROR", e.message ?: "Upstream API error")
            is ServerBusyException -> ErrorSchedule("SERVER_BUSY", e.message ?: "Server is busy")
            else -> {
                logger.error("Unexpected error in ScheduleResolver", e)
                ErrorSchedule("INTERNAL_ERROR", "An unexpected error occurred")
            }
        }
    }
}