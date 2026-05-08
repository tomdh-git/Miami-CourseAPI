package com.tomdh.courseapi.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Typed configuration for the CourseAPI application.
 * All tunable values are externalized here so they can be overridden
 * per environment via application.yml or environment variables.
 */
@ConfigurationProperties(prefix = "courseapi")
data class CourseApiProperties(
    val cache: CacheProperties = CacheProperties(),
    val graphql: GraphQlProperties = GraphQlProperties(),
    val schedule: ScheduleProperties = ScheduleProperties()
) {
    data class CacheProperties(
        /** Cache entry TTL in minutes */
        val ttlMinutes: Long = 60,
        /** Maximum number of entries per cache */
        val maxSize: Long = 1000
    )

    data class GraphQlProperties(
        /** Maximum allowed query depth (prevents abuse) */
        val maxQueryDepth: Int = 10,
        /** Default result limit when the client doesn't specify one */
        val defaultResultLimit: Int = 100
    )

    data class ScheduleProperties(
        /** Maximum number of schedule combinations the combinator will produce */
        val maxCombinatorResults: Int = 100
    )
}
