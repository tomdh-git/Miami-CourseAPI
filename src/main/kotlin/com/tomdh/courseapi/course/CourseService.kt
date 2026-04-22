package com.tomdh.courseapi.course

import com.tomdh.courseapi.exceptions.types.QueryException
import com.tomdh.courseapi.school.SchoolRegistry
import org.springframework.stereotype.Service

@Service
class CourseService(
    private val validator: CourseValidator,
    private val registry: SchoolRegistry
) {
    suspend fun getCourseByInfo(input: CourseByInfoInput): List<Course> {
        val connector = registry.getConnector(input.school)
        validator.validateCourseFields(
            input,
            connector.getOrFetchValidFields()
        )
        return connector.getCourseByInfo(input)
            .ifEmpty { throw QueryException("Desired course does not exist") }
    }

    suspend fun getCourseByCRN(input: CourseByCRNInput): List<Course> {
        val connector = registry.getConnector(input.school)
        return connector.getCourseByCRN(input)
            .ifEmpty { throw QueryException("Desired course does not exist") }
    }
}
