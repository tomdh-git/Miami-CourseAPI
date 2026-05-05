package com.tomdh.courseapi.schedule

import com.tomdh.courseapi.course.SchedulableSection

sealed interface ScheduleResult
data class SuccessSchedule(val schedules: List<Schedule>) : ScheduleResult
data class ErrorSchedule(val error: String, val message: String) : ScheduleResult

data class Schedule(
    val sections: List<SchedulableSection>,
    val freeTime: Int
)

/**
 * Unified input for schedule generation and filler search.
 * If [fillerFilters] is provided, the combinator will also search for
 * filler courses matching those criteria.
 */
data class ScheduleQueryInput(
    val school: String,
    val filters: Map<String, Any?>,
    val courses: List<String>,
    val optimizeFreeTime: Boolean? = false,
    val preferredStart: String? = null,
    val preferredEnd: String? = null,
    val fillerFilters: Map<String, Any?>? = null
)
