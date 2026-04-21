package com.example.courseapi.config

import com.example.courseapi.course.model.Course
import com.example.courseapi.field.model.Field
import com.example.courseapi.field.model.ValidFields

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
