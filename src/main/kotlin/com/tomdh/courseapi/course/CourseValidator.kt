package com.tomdh.courseapi.course

import com.tomdh.courseapi.field.ValidFields
import org.springframework.stereotype.Component

@Component
class CourseValidator {
    fun validateCourseFields(input: CourseByInfoInput, fields: ValidFields) {
        if (input.campus.isEmpty() || !input.campus.all { it in fields.campuses }) throw IllegalArgumentException("Campuses empty or invalid")
        if (input.term.isEmpty() || input.term !in fields.terms) throw IllegalArgumentException("Term is empty or invalid")
        if (!(input.subject.isNullOrEmpty() || input.subject.all { it in fields.subjects })) throw IllegalArgumentException("Invalid subjects")
        if (!input.courseNum.isNullOrEmpty()) {
            if (input.subject.isNullOrEmpty()) throw IllegalArgumentException("Course num without subject")
            if (input.subject.size > 1) throw IllegalArgumentException("Course num with multiple subjects")
        }
        if (!input.attributes.isNullOrEmpty() && !input.attributes.all { it in fields.attributes }) throw IllegalArgumentException("Attributes invalid")
        if (!input.delivery.isNullOrEmpty() && !input.delivery.all { it in fields.deliveryTypes }) throw IllegalArgumentException("Delivery invalid")
        if (!input.startEndTime.isNullOrEmpty() && input.startEndTime.size != 2) throw IllegalArgumentException("StartEndTime invalid")
        if (!(input.openWaitlist.isNullOrEmpty() || input.openWaitlist in fields.waitlistTypes)) throw IllegalArgumentException("Invalid openWaitlist")
        if (!(input.level.isNullOrEmpty() || input.level in fields.levels)) throw IllegalArgumentException("Invalid level")
        if (!(input.daysFilter.isNullOrEmpty() || input.daysFilter.all { it in fields.days })) throw IllegalArgumentException("Invalid daysFilter")
    }
}
