package com.example.courseapi.course

import com.example.courseapi.exceptions.*
import com.example.courseapi.config.SchoolRegistry
import org.springframework.stereotype.Service

@Service
class CourseService(
    private val validator: CourseValidator,
    private val registry: SchoolRegistry
) {
    suspend fun getCourseByInfo(input: CourseByInfoInput): List<Course> {
        val connector = registry.getConnector(input.school)
        validator.validateCourseFields(input, connector.getOrFetchValidFields())
        val res = connector.getCourseByInfo(input)
        return res.ifEmpty { throw QueryException("Desired course does not exist or no courses found") }
    }

    suspend fun getCourseByCRN(input: CourseByCRNInput): List<Course> {
        val connector = registry.getConnector(input.school)
        val res = connector.getCourseByCRN(input)
        return res.ifEmpty { throw QueryException("Desired course does not exist or no courses found") }
    }
}
