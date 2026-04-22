package com.tomdh.courseapi.schedule

import com.tomdh.courseapi.course.Course

sealed interface ScheduleResult
data class SuccessSchedule(val schedules: List<Schedule>) : ScheduleResult
data class ErrorSchedule(val error: String, val message: String) : ScheduleResult

data class Schedule(
    val courses: List<Course>,
    val freeTime: Int
)

data class ScheduleByCourseInput(
    val school: String = "miami",
    val delivery: List<String>? = null,
    val courses: List<String>,
    val campus: List<String>,
    val term: String,
    val optimizeFreeTime: Boolean? = false,
    val preferredStart: String? = null,
    val preferredEnd: String? = null
)

data class FillerByAttributesInput(
    val school: String = "miami",
    val delivery: List<String>? = null,
    val attributes: List<String>,
    val courses: List<String>,
    val campus: List<String>,
    val term: String,
    val preferredStart: String? = null,
    val preferredEnd: String? = null
) {
    fun toScheduleInput() = ScheduleByCourseInput(school, delivery, courses, campus, term, true, preferredStart, preferredEnd)
}
