package com.example.courseapi.config

import com.example.courseapi.course.model.Course
import com.example.courseapi.course.model.input.CourseByInfoInput
import com.example.courseapi.course.model.input.CourseByCRNInput
import com.example.courseapi.field.model.Field
import com.example.courseapi.field.model.ValidFields

/**
 * Contract for a school data source connector.
 * Each school (e.g., Miami, OSU) implements this interface
 * and registers itself via Spring component scanning.
 */
interface SchoolConnector {
    val schoolId: String

    /** Whether this school supports schedule generation. */
    fun supportsScheduling(): Boolean = true

    /** Check if the upstream data source is reachable. */
    suspend fun isAvailable(): Boolean

    suspend fun getCourseByInfo(input: CourseByInfoInput): List<Course>
    suspend fun getCourseByCRN(input: CourseByCRNInput): List<Course>
    suspend fun getOrFetchValidFields(): ValidFields
    suspend fun getTerms(): List<Field>
}
