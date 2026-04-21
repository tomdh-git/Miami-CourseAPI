package com.tomdh.courseapi.school.miami

import com.tomdh.courseapi.school.SchoolConnector
import com.tomdh.courseapi.course.model.Course
import com.tomdh.courseapi.course.model.input.CourseByCRNInput
import com.tomdh.courseapi.course.model.input.CourseByInfoInput
import com.tomdh.courseapi.field.model.Field
import com.tomdh.courseapi.field.model.ValidFields
import com.tomdh.courseapi.school.miami.repo.MiamiCourseRepo
import com.tomdh.courseapi.school.miami.repo.MiamiFieldRepo
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class MiamiConnector(
    private val courseRepo: MiamiCourseRepo,
    private val fieldRepo: MiamiFieldRepo,
    private val client: com.tomdh.courseapi.school.miami.client.MiamiClient
) : SchoolConnector {

    private val logger = LoggerFactory.getLogger(MiamiConnector::class.java)

    override val schoolId: String = "miami"

    override fun supportsScheduling(): Boolean = true

    override suspend fun isAvailable(): Boolean {
        return try {
            val html = client.getCourseList()
            html.isNotEmpty()
        } catch (e: Exception) {
            logger.warn("Miami availability check failed", e)
            false
        }
    }

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
