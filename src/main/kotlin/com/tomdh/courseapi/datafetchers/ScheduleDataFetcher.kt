package com.tomdh.courseapi.datafetchers

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import com.tomdh.courseapi.config.CourseApiProperties
import com.tomdh.courseapi.exceptions.resolveQuery
import com.tomdh.courseapi.generated.types.ErrorSchedule
import com.tomdh.courseapi.generated.types.ScheduleQueryInput
import com.tomdh.courseapi.generated.types.ScheduleResult
import com.tomdh.courseapi.generated.types.SuccessSchedule
import com.tomdh.courseapi.service.ScheduleService
import org.slf4j.LoggerFactory

@DgsComponent
class ScheduleDataFetcher(
    private val scheduleService: ScheduleService,
    private val properties: CourseApiProperties
) {
    private val logger = LoggerFactory.getLogger(ScheduleDataFetcher::class.java)

    @DgsQuery
    suspend fun getSchedules(
        @InputArgument input: ScheduleQueryInput,
        @InputArgument limit: Int?
    ): ScheduleResult =
        resolveQuery(logger = logger, errorFactory = ::ErrorSchedule) {
            SuccessSchedule(
                schedules = scheduleService.getSchedules(input)
                    .take(limit ?: properties.graphql.defaultResultLimit)
            )
        }
}
