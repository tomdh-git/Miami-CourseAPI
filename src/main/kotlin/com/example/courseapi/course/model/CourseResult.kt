package com.example.courseapi.course.model

sealed interface CourseResult
data class SuccessCourse(val courses: List<Course>) : CourseResult
data class ErrorCourse(val error: String, val message: String) : CourseResult
