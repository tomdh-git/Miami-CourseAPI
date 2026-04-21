package com.example.courseapi.schedule

import com.example.courseapi.schedule.FillerByAttributesInput
import com.example.courseapi.schedule.ScheduleByCourseInput
import com.example.courseapi.schedule.Schedule
import com.example.courseapi.config.SchoolRegistry
import org.springframework.stereotype.Service

@Service
class ScheduleService(
    private val combinator: ScheduleCombinator,
    private val validator: ScheduleValidator,
    private val registry: SchoolRegistry
) {
    suspend fun getScheduleByCourses(input: ScheduleByCourseInput): List<Schedule> {
        val connector = registry.getConnector(input.school)
        val fields = connector.getOrFetchValidFields()
        validator.validateScheduleFields(input, fields)
        return combinator.getScheduleByCourses(input, connector)
    }

    suspend fun getFillerByAttributes(input: FillerByAttributesInput): List<Schedule> {
        val connector = registry.getConnector(input.school)
        val fields = connector.getOrFetchValidFields()
        validator.validateScheduleFields(input.toScheduleInput(), fields, attributes = input.attributes)
        return combinator.getFillerByAttributes(input, connector)
    }
}
