package com.tomdh.courseapi.schedule

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import com.tomdh.courseapi.config.CourseApiProperties
import com.tomdh.courseapi.exceptions.resolveQuery
import com.tomdh.courseapi.generated.types.ErrorSchedule
import com.tomdh.courseapi.generated.types.ScheduleQueryInput
import com.tomdh.courseapi.generated.types.ScheduleResult
import com.tomdh.courseapi.generated.types.SuccessSchedule
import org.slf4j.LoggerFactory

@DgsComponent
class ScheduleResolver(
    private val service: ScheduleService,
    private val properties: CourseApiProperties
) {
    private val logger = LoggerFactory.getLogger(ScheduleResolver::class.java)

    @DgsQuery
    suspend fun getSchedules(
        @InputArgument input: ScheduleQueryInput,
        @InputArgument limit: Int?
    ): ScheduleResult {
        return resolveQuery(logger, ::ErrorSchedule) {
            SuccessSchedule(
                service.getSchedules(input)
                    .take(limit ?: properties.graphql.defaultResultLimit)
            )
        }
    }
}
