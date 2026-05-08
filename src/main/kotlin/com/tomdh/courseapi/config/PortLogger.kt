package com.tomdh.courseapi.config

import org.springframework.boot.web.context.WebServerInitializedEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

@Component
class PortLogger : ApplicationListener<WebServerInitializedEvent> {
    override fun onApplicationEvent(event: WebServerInitializedEvent) {
        val port = event.webServer.port
        println("SERVER_PORT=$port")
    }
}
