package com.tomdh.courseapi.course

sealed interface CourseResult
data class SuccessCourse(val courses: List<Course>) : CourseResult
data class ErrorCourse(val error: String, val message: String) : CourseResult

data class Course(
    val subject: String = "",
    val courseNum: String = "",
    val title: String = "",
    val section: String = "",
    val crn: Int = 0,
    val campus: String = "",
    val credits: Int = 0,
    val capacity: String = "",
    val requests: String = "",
    val delivery: String = ""
)

data class CourseByCRNInput(
    val school: String = "miami",
    val crn: Int,
    val term: String
)

data class CourseByInfoInput(
    val school: String = "miami",
    val subject: List<String>? = null,
    val courseNum: String? = null,
    val campus: List<String>,
    val attributes: List<String>? = null,
    val delivery: List<String>? = null,
    val term: String,
    val openWaitlist: String? = null,
    val crn: Int? = null,
    val partOfTerm: List<String>? = null,
    val level: String? = null,
    val courseTitle: String? = null,
    val daysFilter: List<String>? = null,
    val creditHours: Int? = null,
    val startEndTime: List<String>? = null
)
