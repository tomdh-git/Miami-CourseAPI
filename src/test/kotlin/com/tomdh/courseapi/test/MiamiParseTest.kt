package com.tomdh.courseapi.test

import com.tomdh.courseapi.course.model.input.CourseByInfoInput
import com.tomdh.courseapi.school.miami.client.MiamiClient
import com.tomdh.courseapi.school.miami.MiamiConfig
import com.tomdh.courseapi.school.miami.parser.MiamiCourseParser
import com.tomdh.courseapi.school.miami.repo.MiamiCourseRepo
import kotlinx.coroutines.runBlocking
import org.springframework.web.reactive.function.client.WebClient

fun main() = runBlocking {
    val client = MiamiClient(WebClient.create(), MiamiConfig())
    client.warmUpConnection()
    val parser = MiamiCourseParser()
    val repo = MiamiCourseRepo(client, parser)
    
    try {
        val courses = repo.getCourseByInfo(CourseByInfoInput(
            school = "miami",
            term = "202620",
            campus = listOf("O", "H", "M"),
            subject = listOf("CSE"),
            courseNum = "174"
        ))
        println("Found \${courses.size} courses.")
        if (courses.isNotEmpty()) {
            println("Course 1: \${courses[0]}")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
