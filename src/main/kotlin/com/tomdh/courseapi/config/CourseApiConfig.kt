package com.tomdh.courseapi.config

import com.github.benmanes.caffeine.cache.Caffeine
import graphql.analysis.MaxQueryDepthInstrumentation
import org.springframework.boot.web.context.WebServerInitializedEvent
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
class CourseApiConfig(private val properties: CourseApiProperties) {

    @Bean
    fun cacheManager(): CacheManager {
        val cacheManager = CaffeineCacheManager("courses", "schedules", "fillerAttributes")
        cacheManager.setCaffeine(
            Caffeine.newBuilder()
                .expireAfterWrite(properties.cache.ttlMinutes, TimeUnit.MINUTES)
                .maximumSize(properties.cache.maxSize)
        )
        return cacheManager
    }

    @Bean
    fun maxQueryDepthInstrumentation(): MaxQueryDepthInstrumentation =
        MaxQueryDepthInstrumentation(properties.graphql.maxQueryDepth)
}

@Configuration
@Profile("dev", "default")
class CorsConfig {

    @Bean
    fun corsConfigurer(): WebMvcConfigurer =
        object : WebMvcConfigurer {
            override fun addCorsMappings(registry: CorsRegistry) {
                registry.addMapping("/**")
                    .allowedOrigins("*")
                    .allowedMethods("*")
                    .allowedHeaders("*")
            }
        }
}

@Component
class PortLogger : ApplicationListener<WebServerInitializedEvent> {
    override fun onApplicationEvent(event: WebServerInitializedEvent) {
        val port: Int = event.webServer.port
        println("SERVER_PORT=$port")
    }
}
