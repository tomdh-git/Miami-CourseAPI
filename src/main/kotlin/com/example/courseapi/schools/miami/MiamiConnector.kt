package com.example.courseapi.schools.miami

import com.example.courseapi.config.SchoolConnector
import com.example.courseapi.course.Course
import com.example.courseapi.course.CourseByCRNInput
import com.example.courseapi.course.CourseByInfoInput
import com.example.courseapi.field.Field
import com.example.courseapi.field.ValidFields
import org.springframework.stereotype.Component

@Component
class MiamiConnector(
    private val courseRepo: MiamiCourseRepo,
    private val fieldRepo: MiamiFieldRepo
) : SchoolConnector {
    
    override val schoolId: String = "miami"
    
    override suspend fun getCourseByInfo(input: CourseByInfoInput): List<Course> {
        return courseRepo.getCourseByInfo(input)
    }

    override suspend fun getCourseByCRN(input: CourseByCRNInput): List<Course> {
        return courseRepo.getCourseByInfo(
            CourseByInfoInput(crn = input.crn, term = input.term, campus = listOf("All"))
        )
    }

    override suspend fun getOrFetchValidFields(): ValidFields {
        return fieldRepo.getOrFetchValidFields()
    }

    override suspend fun getTerms(): List<Field> {
        return fieldRepo.getTerms()
    }
}
