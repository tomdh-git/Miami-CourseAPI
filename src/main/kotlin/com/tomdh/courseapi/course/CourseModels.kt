package com.tomdh.courseapi.course

/**
 * GraphQL input wrapper. DGS maps the JSON scalar to Map<String, Any?>.
 */
data class CourseQueryInput(
    val school: String,
    val filters: Map<String, Any?>
)

sealed interface CourseResult
data class SuccessCourse(val courses: List<SchedulableSection>) : CourseResult
data class ErrorCourse(val error: String, val message: String) : CourseResult
