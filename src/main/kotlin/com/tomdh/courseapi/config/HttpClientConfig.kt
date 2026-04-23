package com.tomdh.courseapi.config

import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import java.util.concurrent.TimeUnit.SECONDS

@org.springframework.context.annotation.Configuration
class HttpClientConfig(private val miamiConfig: com.tomdh.courseapi.school.miami.MiamiConfig) {

    @org.springframework.context.annotation.Bean
    fun webClient(builder: WebClient.Builder): WebClient {
        val httpClient = reactor.netty.http.client.HttpClient.create()
            .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, miamiConfig.connectTimeoutMs)
            .responseTimeout(java.time.Duration.ofSeconds(miamiConfig.readWriteTimeoutSec))
            .doOnConnected { conn ->
                conn.addHandlerLast(ReadTimeoutHandler(
                    miamiConfig.readWriteTimeoutSec,
                    SECONDS)
                )
                conn.addHandlerLast(WriteTimeoutHandler(
                    miamiConfig.readWriteTimeoutSec,
                    SECONDS)
                )
            }
            .followRedirect(true)

        return builder
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .codecs { configurer ->
                configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)
            }
            .build()
    }
}
