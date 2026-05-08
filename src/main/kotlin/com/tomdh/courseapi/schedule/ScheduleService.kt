package com.tomdh.courseapi.schedule

import com.tomdh.courseapi.exceptions.types.ValidationException
import com.tomdh.courseapi.generated.types.Schedule
import com.tomdh.courseapi.generated.types.ScheduleQueryInput
import com.tomdh.courseapi.schedule.combinator.ScheduleCombinator
import com.tomdh.schoolconnector.school.SchoolRegistry
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

/**
 * Contract for generating schedule combinations.
 */
interface ScheduleService {
    suspend fun getSchedules(input: ScheduleQueryInput): List<Schedule>
}

@Service
class DefaultScheduleService(
    private val combinator: ScheduleCombinator,
    private val registry: SchoolRegistry
) : ScheduleService {
    /**
     * Generates schedules, optionally with filler courses.
     * If [ScheduleQueryInput.fillerFilters] is provided, also searches for fillers.
     */
    @Cacheable(value = ["schedules"], key = "{#input}")
    override suspend fun getSchedules(input: ScheduleQueryInput): List<Schedule> {
        val connector = registry.getConnector(input.school)

        @Suppress("UNCHECKED_CAST")
        val filters = input.filters as Map<String, Any?>

        // Validate the base filters
        val errors = connector.validateFilters(filters)
        if (errors.isNotEmpty()) throw ValidationException(errors)

        @Suppress("UNCHECKED_CAST")
        val fillerFilters = input.fillerFilters as? Map<String, Any?>
        return if (fillerFilters != null) {
            combinator.getFillerSchedules(input, connector)
        } else {
            combinator.getScheduleByCourses(input, connector)
        }
    }
}
