package com.example.courseapi.schedule

import com.example.courseapi.course.Course

data class Schedule(
    val courses: List<Course>,
    val freeTime: Int
)
