package com.tomdh.courseapi.schedule.api

import com.tomdh.courseapi.field.model.ValidFields
import com.tomdh.courseapi.schedule.model.input.ScheduleByCourseInput
import org.springframework.stereotype.Component

@Component
class ScheduleValidator {

    fun validateScheduleFields(input: ScheduleByCourseInput, fields: ValidFields, attributes: List<String>? = null) {
        if (input.campus.isEmpty() || !input.campus.all { it in fields.campuses }) throw IllegalArgumentException("Campuses empty or invalid")
        if (input.term.isEmpty() || input.term !in fields.terms) throw IllegalArgumentException("Term is empty or invalid")
        if (!input.delivery.isNullOrEmpty() && !input.delivery.all { it in fields.deliveryTypes }) throw IllegalArgumentException("Delivery types invalid")
        if (!attributes.isNullOrEmpty() && !attributes.all { it in fields.attributes }) throw IllegalArgumentException("Attributes field invalid")
        if ((!input.preferredStart.isNullOrEmpty() && input.preferredEnd.isNullOrEmpty())
            || (!input.preferredEnd.isNullOrEmpty() && input.preferredStart.isNullOrEmpty())) throw IllegalArgumentException("Preferred start and end fields must be specified together")
        if (input.preferredStart != null && input.preferredEnd != null) {
            try {
                val formatter = java.time.format.DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("h:mma").toFormatter()
                val start = java.time.LocalTime.parse(input.preferredStart.replace(" ", ""), formatter)
                val end = java.time.LocalTime.parse(input.preferredEnd.replace(" ", ""), formatter)
                if (start >= end) throw IllegalArgumentException("Preferred start must be before preferred end")
            } catch (e: Exception) {
                // Ignore parse errors here, let them fail in combinator
            }
        }
    }
}