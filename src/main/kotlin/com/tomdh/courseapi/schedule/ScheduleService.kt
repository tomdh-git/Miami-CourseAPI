package com.tomdh.courseapi.schedule

import com.tomdh.courseapi.exceptions.types.ValidationException
import com.tomdh.schoolconnector.school.SchoolRegistry
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class ScheduleService(
    private val combinator: ScheduleCombinator,
    private val registry: SchoolRegistry
) {
    /**
     * Generates schedules, optionally with filler courses.
     * If [ScheduleQueryInput.fillerFilters] is provided, also searches for fillers.
     */
    @Cacheable(value = ["schedules"], key = "{#input}")
    suspend fun getSchedules(input: ScheduleQueryInput): List<Schedule> {
        val connector = registry.getConnector(input.school)

        // Validate the base filters
        val errors = connector.validateFilters(input.filters)
        if (errors.isNotEmpty()) throw ValidationException(errors)

        return if (input.fillerFilters != null) {
            combinator.getFillerSchedules(input, connector)
        } else {
            combinator.getScheduleByCourses(input, connector)
        }
    }
}
