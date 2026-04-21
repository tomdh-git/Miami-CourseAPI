package com.tomdh.courseapi.schedule.service

import com.tomdh.courseapi.school.SchoolRegistry
import com.tomdh.courseapi.course.model.Course
import com.tomdh.courseapi.course.model.input.CourseByInfoInput
import com.tomdh.courseapi.schedule.model.input.FillerByAttributesInput
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory

@Component
class FillerAttributeCache(private val registry: SchoolRegistry) {
    private val logger = LoggerFactory.getLogger(FillerAttributeCache::class.java)

    @Cacheable("fillerAttributes")
    suspend fun fetchAttributes(input: FillerByAttributesInput): List<Course> {
        logger.info("Cache miss for filler attributes. Fetching combinations...")
        val startAndEndTimeValid = input.preferredStart != null && input.preferredEnd != null
        val startEndTime = if (startAndEndTimeValid) {
            listOf(input.preferredStart, input.preferredEnd)
        } else null

        val connector = registry.getConnector(input.school)
        return connector.getCourseByInfo(CourseByInfoInput(
            school = input.school,
            campus = input.campus,
            term = input.term,
            attributes = input.attributes,
            delivery = input.delivery,
            startEndTime = startEndTime
        ))
    }
}
