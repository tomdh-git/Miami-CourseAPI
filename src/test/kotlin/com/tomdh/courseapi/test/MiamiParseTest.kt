package com.tomdh.courseapi.test

import com.tomdh.courseapi.course.CourseByInfoInput
import com.tomdh.courseapi.school.miami.MiamiClient
import com.tomdh.courseapi.school.miami.MiamiConfig
import com.tomdh.courseapi.school.miami.MiamiConnector
import kotlinx.coroutines.runBlocking
import org.springframework.web.reactive.function.client.WebClient

fun main() = runBlocking {
    val config = MiamiConfig()
    val client = MiamiClient(WebClient.create(), config)
    client.warmUpConnection()
    val connector = MiamiConnector(client, config)

    try {
        val courses = connector.getCourseByInfo(CourseByInfoInput(
            school = "miami", term = "202620",
            campus = listOf("O", "H", "M"),
            subject = listOf("CSE"), courseNum = "174"
        ))
        println("Found ${courses.size} courses.")
        if (courses.isNotEmpty()) println("Course 1: ${courses[0]}")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
