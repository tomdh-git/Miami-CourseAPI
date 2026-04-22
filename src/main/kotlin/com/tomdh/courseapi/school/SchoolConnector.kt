package com.tomdh.courseapi.school

import com.tomdh.courseapi.course.Course
import com.tomdh.courseapi.course.CourseByInfoInput
import com.tomdh.courseapi.course.CourseByCRNInput
import com.tomdh.courseapi.field.Field
import com.tomdh.courseapi.field.ValidFields

interface SchoolConnector {
    val schoolId: String

    fun supportsScheduling(): Boolean = true

    suspend fun isAvailable(): Boolean

    suspend fun getCourseByInfo(input: CourseByInfoInput): List<Course>
    suspend fun getCourseByCRN(input: CourseByCRNInput): List<Course>
    suspend fun getOrFetchValidFields(): ValidFields
    suspend fun getTerms(): List<Field>
}
