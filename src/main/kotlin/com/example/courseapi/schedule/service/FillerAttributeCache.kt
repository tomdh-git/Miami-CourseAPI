package com.example.courseapi.schedule

import com.example.courseapi.config.SchoolRegistry
import com.example.courseapi.course.Course
import com.example.courseapi.course.CourseByInfoInput
import org.springframework.stereotype.Component

import org.springframework.cache.annotation.Cacheable
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
