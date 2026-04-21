package com.tomdh.courseapi.schedule.service

import com.tomdh.courseapi.schedule.model.Schedule
import com.tomdh.courseapi.schedule.model.input.FillerByAttributesInput
import com.tomdh.courseapi.schedule.model.input.ScheduleByCourseInput
import com.tomdh.courseapi.school.SchoolRegistry
import com.tomdh.courseapi.schedule.api.ScheduleValidator
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
