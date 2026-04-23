package com.tomdh.courseapi.config

@org.springframework.context.annotation.Configuration
@org.springframework.cache.annotation.EnableCaching
class CacheConfig {

    @org.springframework.context.annotation.Bean
    fun cacheManager(): org.springframework.cache.CacheManager {
        val caffeine = com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
            .expireAfterWrite(1, java.util.concurrent.TimeUnit.HOURS)
            .maximumSize(1000)
            
        val cacheManager = org.springframework.cache.caffeine.CaffeineCacheManager(
            "courses",
            "schedules",
            "fillerAttributes"
        )
        cacheManager.setCaffeine(caffeine)
        return cacheManager
    }
}
