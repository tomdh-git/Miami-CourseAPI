package com.tomdh.courseapi.config

import graphql.analysis.MaxQueryDepthInstrumentation
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GraphQLConfig {

    @Bean
    fun maxQueryDepthInstrumentation(): MaxQueryDepthInstrumentation {
        return MaxQueryDepthInstrumentation(10)
    }
}
