package com.example.courseapi.schedule

import com.example.courseapi.field.ValidFields
import org.springframework.stereotype.Component

@Component
class ScheduleValidator {

    fun validateScheduleFields(input: ScheduleByCourseInput, fields: ValidFields, attributes: List<String>?=null) {
    if (input.campus.isEmpty() || !input.campus.all { it in fields.campuses }) throw IllegalArgumentException("Campuses empty or invalid")
    if (input.term.isEmpty() || input.term !in fields.terms) throw IllegalArgumentException("Term is empty or invalid")
    if (!input.delivery.isNullOrEmpty() && !input.delivery.all { it in fields.deliveryTypes }) throw IllegalArgumentException("Delivery types invalid")
    if (!attributes.isNullOrEmpty() && !attributes.all { it in fields.attributes })  throw IllegalArgumentException("Attributes field invalid")
    if ((!input.preferredStart.isNullOrEmpty()&&input.preferredEnd.isNullOrEmpty())
        ||(!input.preferredEnd.isNullOrEmpty() && input.preferredStart.isNullOrEmpty())) throw IllegalArgumentException("Preferred start and end fields must be specified together")
        if ((input.preferredStart!=null && input.preferredEnd!=null)&&
            toMinutes(input.preferredStart)>=toMinutes(input.preferredEnd)) throw IllegalArgumentException("Preferred start must be before preferred end")
    }
}