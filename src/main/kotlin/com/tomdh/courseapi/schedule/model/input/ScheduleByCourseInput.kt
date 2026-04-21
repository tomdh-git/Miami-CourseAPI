package com.tomdh.courseapi.schedule.model.input

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
