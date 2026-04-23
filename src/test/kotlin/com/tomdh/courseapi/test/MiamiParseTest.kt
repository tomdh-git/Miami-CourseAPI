package com.tomdh.courseapi.test

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
        val courses = connector.queryCourses(mapOf(
            "term" to "202620",
            "campus" to listOf("O", "H", "M"),
            "subject" to listOf("CSE"),
            "courseNum" to "174"
        ))
        println("Found ${courses.size} sections.")
        if (courses.isNotEmpty()) println("Section 1: ${courses[0]}")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
