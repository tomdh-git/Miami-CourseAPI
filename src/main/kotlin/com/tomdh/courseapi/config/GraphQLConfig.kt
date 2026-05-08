package com.tomdh.courseapi.config

import graphql.analysis.MaxQueryDepthInstrumentation
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GraphQLConfig(private val properties: CourseApiProperties) {

    @Bean
    fun maxQueryDepthInstrumentation(): MaxQueryDepthInstrumentation {
        return MaxQueryDepthInstrumentation(properties.graphql.maxQueryDepth)
    }
}
