package com.tomdh.courseapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.tomdh.courseapi", "com.tomdh.schoolconnector", "com.tomdh.sessionawarewebclient"])
@ConfigurationPropertiesScan(basePackages = ["com.tomdh.courseapi", "com.tomdh.schoolconnector", "com.tomdh.sessionawarewebclient"])
class CourseApiApplication

fun main(args: Array<String>) {
    runApplication<CourseApiApplication>(*args)
}
