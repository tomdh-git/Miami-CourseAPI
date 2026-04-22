package com.tomdh.courseapi.school.miami

import com.tomdh.courseapi.exceptions.types.ServerBusyException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

class MiamiClientTests {

    private lateinit var exchangeFunction: ExchangeFunction
    private lateinit var webClient: WebClient
    private lateinit var config: MiamiConfig
    private lateinit var client: MiamiClient

    @BeforeEach
    fun setup() {
        exchangeFunction = mock()
        webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
        config = MiamiConfig(
            url = "http://test.local",
            htmlCacheTimeoutMs = 1000,
            tokenTimeoutMs = 1000,
            tokenRefreshThresholdMs = 500,
            requestTimeoutMs = 5000
        )
        client = MiamiClient(webClient, config)
    }

    private fun mockResponse(body: String, status: HttpStatus = HttpStatus.OK) {
        val response = ClientResponse.create(status).body(body).build()
        whenever(exchangeFunction.exchange(any())).thenReturn(Mono.just(response))
    }
    
    private fun mockDelayedResponse(body: String, delayMs: Long) {
        val response = ClientResponse.create(HttpStatus.OK).body(body).build()
        whenever(exchangeFunction.exchange(any())).thenAnswer {
            Mono.just(response).delayElement(java.time.Duration.ofMillis(delayMs))
        }
    }

    @Test
    fun `warmUpConnection fetches HTML and loads token`() = runBlocking {
        mockResponse("""<input name="_token" value="test-token-123">""")
        client.warmUpConnection()
        delay(100) // allow launch to finish

        val token = client.getOrFetchToken()
        assertEquals("test-token-123", token)
    }

    @Test
    fun `getCourseList uses cache`() = runBlocking {
        mockResponse("""first""")
        val r1 = client.getCourseList()
        
        mockResponse("""second""")
        val r2 = client.getCourseList() // should use cache
        
        assertEquals("first", r1)
        assertEquals("first", r2)
    }

    @Test
    fun `getCourseList forceFresh bypasses cache`() = runBlocking {
        mockResponse("""first""")
        client.getCourseList()
        
        mockResponse("""second""")
        val r2 = client.getCourseList(forceFresh = true)
        
        assertEquals("second", r2)
    }

    @Test
    fun `postResultResponse throws ServerBusyException on timeout`() = runBlocking {
        val lowTimeoutConfig = config.copy(requestTimeoutMs = 100) // low timeout
        val timeoutClient = MiamiClient(webClient, lowTimeoutConfig)
        mockDelayedResponse("success", 300) // response takes longer than timeout
        
        val e = assertThrows<ServerBusyException> {
            timeoutClient.postResultResponse("data")
        }
        assertTrue(e.message!!.contains("timed out"))
    }
}
