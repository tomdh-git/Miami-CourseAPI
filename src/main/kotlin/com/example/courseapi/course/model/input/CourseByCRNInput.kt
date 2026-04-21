package com.example.courseapi.course.model.input

data class CourseByCRNInput(
    val school: String = "miami",
    val crn: Int,
    val term: String
)
