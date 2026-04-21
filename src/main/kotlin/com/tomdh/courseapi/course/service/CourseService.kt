package com.tomdh.courseapi.course.service

import com.tomdh.courseapi.course.api.CourseValidator
import com.tomdh.courseapi.course.model.Course
import com.tomdh.courseapi.course.model.input.CourseByCRNInput
import com.tomdh.courseapi.course.model.input.CourseByInfoInput
import com.tomdh.courseapi.school.SchoolRegistry
import com.tomdh.courseapi.exceptions.QueryException
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
