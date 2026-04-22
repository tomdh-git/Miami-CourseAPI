package com.tomdh.courseapi.schedule

import com.tomdh.courseapi.course.Course
import com.tomdh.courseapi.course.CourseByInfoInput
import com.tomdh.courseapi.school.SchoolRegistry
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
class FillerAttributeCache(private val registry: SchoolRegistry) {
    private val logger = LoggerFactory.getLogger(FillerAttributeCache::class.java)

    @Cacheable("fillerAttributes")
    suspend fun fetchAttributes(input: FillerByAttributesInput): List<Course> {
        logger.info("Cache miss for filler attributes. Fetching combinations...")
        val startEndTime = if (input.preferredStart != null && input.preferredEnd != null) {
            listOf(input.preferredStart, input.preferredEnd)
        } else null

        val connector = registry.getConnector(input.school)
        return connector.getCourseByInfo(CourseByInfoInput(
            school = input.school, campus = input.campus, term = input.term,
            attributes = input.attributes, delivery = input.delivery, startEndTime = startEndTime
        ))
    }
}
