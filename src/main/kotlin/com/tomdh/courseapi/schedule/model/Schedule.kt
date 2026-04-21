package com.tomdh.courseapi.schedule.model

import com.tomdh.courseapi.course.model.Course

data class Schedule(
    val courses: List<Course>,
    val freeTime: Int
)
