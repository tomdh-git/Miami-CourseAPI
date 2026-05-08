package com.tomdh.courseapi.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
class CacheConfig(private val properties: CourseApiProperties) {

    @Bean
    fun cacheManager(): CacheManager {
        val caffeine = Caffeine.newBuilder()
            .expireAfterWrite(properties.cache.ttlMinutes, TimeUnit.MINUTES)
            .maximumSize(properties.cache.maxSize)

        val cacheManager = CaffeineCacheManager(
            "courses",
            "schedules",
            "fillerAttributes"
        )
        cacheManager.setCaffeine(caffeine)
        return cacheManager
    }
}
