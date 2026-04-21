package com.example.courseapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "miami.api")
class MiamiConfig {
    lateinit var url: String
    var tokenTimeoutMs: Long = 45000
    var htmlCacheTimeoutMs: Long = 30000
}
