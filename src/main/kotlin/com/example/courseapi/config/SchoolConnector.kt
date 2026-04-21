package com.example.courseapi.config

import com.example.courseapi.course.Course
import com.example.courseapi.course.CourseByInfoInput
import com.example.courseapi.course.CourseByCRNInput
import com.example.courseapi.field.Field
import com.example.courseapi.field.ValidFields

interface SchoolConnector {
    val schoolId: String
    
    suspend fun getCourseByInfo(input: CourseByInfoInput): List<Course>
    suspend fun getCourseByCRN(input: CourseByCRNInput): List<Course>
    suspend fun getOrFetchValidFields(): ValidFields
    suspend fun getTerms(): List<Field>
}
