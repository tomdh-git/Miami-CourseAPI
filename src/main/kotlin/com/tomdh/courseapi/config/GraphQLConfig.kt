package com.tomdh.courseapi.config

import graphql.analysis.MaxQueryDepthInstrumentation

@org.springframework.context.annotation.Configuration
class GraphQLConfig {

    @org.springframework.context.annotation.Bean
    fun maxQueryDepthInstrumentation(): MaxQueryDepthInstrumentation {
        return MaxQueryDepthInstrumentation(10)
    }
}
