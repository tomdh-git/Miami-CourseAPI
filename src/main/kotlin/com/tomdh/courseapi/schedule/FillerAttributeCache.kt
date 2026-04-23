package com.tomdh.courseapi.schedule

@org.springframework.stereotype.Component
class FillerAttributeCache {
    private val logger = org.slf4j.LoggerFactory.getLogger(FillerAttributeCache::class.java)

    /**
     * Fetches filler courses using the school connector and the provided filler filters.
     * Results are cached by the filter map to avoid repeated upstream calls.
     */
    @org.springframework.cache.annotation.Cacheable(value = ["fillerAttributes"], key = "{#connector.schoolId, #fillerFilters}")
    suspend fun fetchFillerCourses(
        connector: com.tomdh.courseapi.school.SchoolConnector,
        fillerFilters: Map<String, Any?>
    ): List<com.tomdh.courseapi.course.SchedulableSection> {
        logger.info("Cache miss for filler courses. Fetching from {}...", connector.schoolId)
        return connector.queryCourses(fillerFilters)
    }
}
