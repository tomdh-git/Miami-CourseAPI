package com.tomdh.courseapi.service.combinator

import com.tomdh.schoolconnector.course.SchedulableSection
import com.tomdh.schoolconnector.school.SchoolConnector
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
class FillerAttributeCache {
  private val logger = LoggerFactory.getLogger(FillerAttributeCache::class.java)

  /**
  * Fetches filler courses using the school connector and the provided filler filters.
  * Results are cached by the filter map to avoid repeated upstream calls.
  */
  @Cacheable(value = ["fillerAttributes"], key = "{#connector.schoolId, #fillerFilters}")
  suspend fun fetchFillerCourses(
    connector: SchoolConnector,
    fillerFilters: Map<String, Any?>
  ): List<SchedulableSection> {
    logger.info("Cache miss for filler courses. Fetching from {}...", connector.schoolId)
    return connector.queryCourses(fillerFilters)
  }
}
