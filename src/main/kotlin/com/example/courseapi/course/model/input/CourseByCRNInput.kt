package com.example.courseapi.course

data class CourseByCRNInput(
    val school: String = "miami",
    val crn: Int,
    val term: String
)
