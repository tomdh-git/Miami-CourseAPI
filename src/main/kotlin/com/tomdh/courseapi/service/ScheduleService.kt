package com.tomdh.courseapi.service

import com.tomdh.courseapi.exceptions.types.ValidationException
import com.tomdh.courseapi.generated.types.Schedule
import com.tomdh.courseapi.generated.types.ScheduleQueryInput
import com.tomdh.courseapi.service.combinator.ScheduleCombinator
import com.tomdh.courseapi.scalar.JsonScalarUtils
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

        val filters: Map<String, Any?> = JsonScalarUtils.safeCastToMap(input.filters)

        // Validate the base filters
        val errors: List<String> = connector.validateFilters(filters)
        if (errors.isNotEmpty()) throw ValidationException(errors)

        val fillerFilters: Map<String, Any?>? = input.fillerFilters?.let { JsonScalarUtils.safeCastToMap(it) }
        return if (!fillerFilters.isNullOrEmpty()) combinator.getFillerSchedules(input, connector)
        else combinator.getScheduleByCourses(input, connector)
    }
}
