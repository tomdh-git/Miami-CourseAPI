package com.tomdh.courseapi.schedule

import com.tomdh.courseapi.school.SchoolRegistry
import org.springframework.stereotype.Service

@Service
class ScheduleService(
    private val combinator: ScheduleCombinator,
    private val validator: ScheduleValidator,
    private val registry: SchoolRegistry
) {
    suspend fun getScheduleByCourses(input: ScheduleByCourseInput): List<Schedule> {
        val connector = registry.getConnector(input.school)
        validator.validateScheduleFields(
            input,
            connector.getOrFetchValidFields()
        )
        return combinator.getScheduleByCourses(input, connector)
    }

    suspend fun getFillerByAttributes(input: FillerByAttributesInput): List<Schedule> {
        val connector = registry.getConnector(input.school)
        validator.validateScheduleFields(
            input.toScheduleInput(),
            connector.getOrFetchValidFields(),
            input.attributes
        )
        return combinator.getFillerByAttributes(input, connector)
    }
}
