package com.tomdh.courseapi.infra

import org.springframework.boot.web.context.WebServerInitializedEvent
import org.springframework.context.ApplicationListener

@org.springframework.stereotype.Component
class PortLogger : ApplicationListener<WebServerInitializedEvent> {
    override fun onApplicationEvent(event: WebServerInitializedEvent) {
        val port = event.webServer.port
        println("SERVER_PORT=$port")
    }
}
