package com.tomdh.courseapi.schedule

import com.tomdh.courseapi.exceptions.resolveQuery
import com.netflix.graphql.dgs.InputArgument

@com.netflix.graphql.dgs.DgsComponent
class ScheduleResolver(private val service: ScheduleService) {
    private val logger = org.slf4j.LoggerFactory.getLogger(ScheduleResolver::class.java)

    @com.netflix.graphql.dgs.DgsQuery
    suspend fun getSchedules(
        @InputArgument input: ScheduleQueryInput,
        @InputArgument limit: Int?
    ): ScheduleResult {
        return resolveQuery(logger, ::ErrorSchedule) {
            SuccessSchedule(
                service.getSchedules(input)
                    .take(limit ?: 100)
            )
        }
    }
}
