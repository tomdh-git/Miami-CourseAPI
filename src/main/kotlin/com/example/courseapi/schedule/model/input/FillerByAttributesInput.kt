package com.example.courseapi.schedule

data class FillerByAttributesInput(
    val school: String = "miami",
    val delivery: List<String>?=null,
    val attributes: List<String>,
    val courses: List<String>,
    val campus: List<String>,
    val term: String,
    val preferredStart: String? = null,
    val preferredEnd: String? = null
){
    fun toScheduleInput(): ScheduleByCourseInput {
        return ScheduleByCourseInput(school, delivery,courses,campus,term,true,preferredStart,preferredEnd)
    }
}
