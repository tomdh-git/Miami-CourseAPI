package com.tomdh.courseapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication(scanBasePackages = ["com.tomdh.courseapi", "com.tomdh.schoolconnector", "com.tomdh.sessionawarewebclient"])
@EnableCaching
@ConfigurationPropertiesScan(basePackages = ["com.tomdh.courseapi", "com.tomdh.schoolconnector", "com.tomdh.sessionawarewebclient"])
class CourseapiApplication

fun main(args: Array<String>) {
    runApplication<CourseapiApplication>(*args)
}
