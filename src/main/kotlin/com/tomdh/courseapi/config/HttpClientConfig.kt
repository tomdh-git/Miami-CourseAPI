package com.tomdh.courseapi.config

import com.tomdh.courseapi.school.miami.MiamiConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import reactor.netty.http.client.HttpClient
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import java.time.Duration
import java.util.concurrent.TimeUnit

@Configuration
class HttpClientConfig(private val miamiConfig: MiamiConfig) {
    @Bean
    fun webClient(builder: WebClient.Builder): WebClient {
        val httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, miamiConfig.connectTimeoutMs)
            .responseTimeout(Duration.ofSeconds(miamiConfig.readWriteTimeoutSec))
            .doOnConnected { conn ->
                conn.addHandlerLast(ReadTimeoutHandler(miamiConfig.readWriteTimeoutSec, TimeUnit.SECONDS))
                conn.addHandlerLast(WriteTimeoutHandler(miamiConfig.readWriteTimeoutSec, TimeUnit.SECONDS))
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
