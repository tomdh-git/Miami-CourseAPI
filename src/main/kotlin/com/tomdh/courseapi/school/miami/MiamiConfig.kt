package com.tomdh.courseapi.school.miami

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "miami.api")
data class MiamiConfig(
    val url: String = "https://www.apps.miamioh.edu/courselist/",
    val tokenTimeoutMs: Long = 45000,
    val htmlCacheTimeoutMs: Long = 30000,
    val fieldsCacheTimeoutMs: Long = 3_600_000,
    val requestTimeoutMs: Long = 28_000,
    val tokenRefreshThresholdMs: Long = 35_000,
    val connectTimeoutMs: Int = 30_000,
    val readWriteTimeoutSec: Long = 60
)
