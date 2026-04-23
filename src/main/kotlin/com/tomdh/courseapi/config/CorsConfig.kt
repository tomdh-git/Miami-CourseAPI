package com.tomdh.courseapi.config

import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@org.springframework.context.annotation.Configuration
class CorsConfig {

    @org.springframework.context.annotation.Bean
    fun corsConfigurer(): WebMvcConfigurer {
        return object : WebMvcConfigurer {
            override fun addCorsMappings(
                registry: org.springframework.web.servlet.config.annotation.CorsRegistry
            ) {
                registry.addMapping("/**")
                    .allowedOrigins("*")
                    .allowedMethods("*")
                    .allowedHeaders("*")
            }
        }
    }
}
