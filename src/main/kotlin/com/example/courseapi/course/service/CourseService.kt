package com.example.courseapi.course.service

import com.example.courseapi.course.api.CourseValidator
import com.example.courseapi.course.model.Course
import com.example.courseapi.course.model.input.CourseByCRNInput
import com.example.courseapi.course.model.input.CourseByInfoInput
import com.example.courseapi.config.SchoolRegistry
import com.example.courseapi.exceptions.QueryException
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
