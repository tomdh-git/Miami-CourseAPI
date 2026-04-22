package com.tomdh.courseapi.schedule

import com.tomdh.courseapi.exceptions.*
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import org.slf4j.LoggerFactory

@DgsComponent
class ScheduleResolver(private val service: ScheduleService) {
    private val logger = LoggerFactory.getLogger(ScheduleResolver::class.java)

    @DgsQuery
    suspend fun getScheduleByCourses(@InputArgument input: ScheduleByCourseInput, @InputArgument limit: Int?): ScheduleResult {
        return resolveQuery(logger, ::ErrorSchedule) {
            SuccessSchedule(
                service.getScheduleByCourses(input)
                    .take(limit ?: 100)
            )
        }
    }

    @DgsQuery
    suspend fun getFillerByAttributes(@InputArgument input: FillerByAttributesInput, @InputArgument limit: Int?): ScheduleResult {
        return resolveQuery(logger, ::ErrorSchedule) {
            SuccessSchedule(
                service.getFillerByAttributes(input)
                    .take(limit ?: 100)
            )
        }
    }
}
