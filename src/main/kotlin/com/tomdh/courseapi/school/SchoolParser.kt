package com.tomdh.courseapi.school

import com.tomdh.courseapi.course.model.Course
import com.tomdh.courseapi.field.model.Field
import com.tomdh.courseapi.field.model.ValidFields

/**
 * Contracts for school-specific HTML/data parsers.
 * Each school implements these to parse its own data format.
 */
interface CourseParser {
    fun parseCourses(raw: String): List<Course>
}

interface FieldParser {
    fun parseTerms(raw: String): List<Field>
    fun parseAllFields(raw: String): ValidFields
}
