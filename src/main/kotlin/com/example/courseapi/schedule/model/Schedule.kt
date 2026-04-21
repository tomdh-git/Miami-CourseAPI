package com.example.courseapi.schedule.model

import com.example.courseapi.course.model.Course

data class Schedule(
    val courses: List<Course>,
    val freeTime: Int
)
