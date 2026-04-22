package com.tomdh.courseapi.school.miami

import com.tomdh.courseapi.exceptions.types.ServerBusyException
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory

@Component
class MiamiClient(private val webClient: WebClient, private val config: MiamiConfig) {
    private val logger = LoggerFactory.getLogger(MiamiClient::class.java)
    @Volatile private var lastToken: String? = null
    @Volatile private var lastTokenTs: Long = 0
    private val tokenLock = Mutex()
    @Volatile private var refreshJob: Job? = null
    private val refreshScope = CoroutineScope(Dispatchers.IO)
    @Volatile private var cachedHtml: String? = null
    @Volatile private var cachedHtmlTs: Long = 0
    private val htmlCacheLock = Mutex()
    private val tokenRegex = Regex("""<input[^>]*name="_token"[^>]*value="([^"]+)"""")

    data class HttpTextResponse(val status: Int, val body: String)

    private val cookies = java.util.concurrent.ConcurrentHashMap<String, String>()

    /**
     * Fetches the course list HTML on application startup to prepopulate
     * the token and html caches, reducing latency for the first actual query.
     */
    @PostConstruct
    fun warmUpConnection() {
        refreshScope.launch {
            try { getCourseList() } catch (e: Exception) { logger.warn("Failed to warm up connection", e) }
        }
    }

    suspend fun getCourseList(forceFresh: Boolean = false): String {
        val now = System.currentTimeMillis()
        val cached = cachedHtml
        val age = now - cachedHtmlTs

        if (!forceFresh && cached != null && age < config.htmlCacheTimeoutMs) return cached

        return htmlCacheLock.withLock {
            val againNow = System.currentTimeMillis()
            val againCached = cachedHtml
            val againAge = againNow - cachedHtmlTs

            val recentlyForced = forceFresh && againCached != null && againAge < 5000
            val requestFreshAgain = (!forceFresh && againCached != null && againAge < config.htmlCacheTimeoutMs) || recentlyForced
            if (requestFreshAgain) return againCached!!

            logger.info("Fetching fresh course list from {}", config.url)
            val result = webClient.get()
                .uri(config.url)
                .header("Accept", "text/html")
                .header("User-Agent", "Mozilla/5.0")
                .exchangeToMono { response ->
                    response.cookies().forEach { (name, cookieList) ->
                        if (cookieList.isNotEmpty()) cookies[name] = cookieList[0].value
                    }
                    response.bodyToMono(String::class.java)
                }.awaitSingle()

            cachedHtml = result
            cachedHtmlTs = System.currentTimeMillis()
            result
        }
    }

    suspend fun getToken(forceFresh: Boolean = false): String {
        val html = getCourseList(forceFresh)
        return tokenRegex.find(html)?.groupValues?.get(1) ?: ""
    }

    suspend fun forceFetchToken(): String = tokenLock.withLock {
        val againNow = System.currentTimeMillis()
        if (lastToken != null && (againNow - lastTokenTs) < 5000) return lastToken!!
        val freshToken = getToken(forceFresh = true)
        if (freshToken.isNotEmpty()) { lastToken = freshToken; lastTokenTs = System.currentTimeMillis() }
        freshToken
    }

    /**
     * Retrieves a valid CSRF token.
     * - Returns cached token if within validity window.
     * - Triggers asynchronous background refresh if approaching expiration.
     * - Blocks and forces a synchronous refresh if expired.
     */
    suspend fun getOrFetchToken(): String {
        val now = System.currentTimeMillis()
        val cached = lastToken
        val age = now - lastTokenTs

        val inWindow = cached != null && age >= config.tokenRefreshThresholdMs && age < config.tokenTimeoutMs
        if (inWindow) {
            val currentJob = refreshJob
            if (currentJob == null || !currentJob.isActive) {
                refreshJob = refreshScope.launch {
                    tokenLock.withLock { lastToken = getToken(); lastTokenTs = System.currentTimeMillis() }
                }
            }
            return cached
        }

        if (cached != null && age < config.tokenTimeoutMs) return cached

        return tokenLock.withLock {
            val againNow = System.currentTimeMillis()
            if (lastToken != null && againNow - lastTokenTs < config.tokenTimeoutMs) return lastToken!!
            val token = getToken()
            lastToken = token; lastTokenTs = againNow; token
        }
    }

    suspend fun postResultResponse(formBody: String): HttpTextResponse {
        val postResponse = getPostResponse(formBody)
        var resultHtml = postResponse.body ?: ""

        if (resultHtml.contains("meta http-equiv=\"refresh\"")) {
            val redirectUrl = """content=\s*"\s*\d+;\s*url='([^']+)'\s*""""
                .toRegex().find(resultHtml)?.groupValues?.get(1)
            if (redirectUrl != null) resultHtml = getRedirectResponseHtml(redirectUrl)
        }
        return HttpTextResponse(postResponse.statusCode.value(), resultHtml)
    }

    private val activeRequests = java.util.concurrent.atomic.AtomicInteger(0)
    private val isBusy = java.util.concurrent.atomic.AtomicBoolean(false)

    private suspend fun getPostResponse(formBody: String): ResponseEntity<String> {
        if (isBusy.get()) throw ServerBusyException("Server is busy, please try again later")
        activeRequests.incrementAndGet()
        return try {
            kotlinx.coroutines.withTimeout(config.requestTimeoutMs) {
                webClient.post()
                    .uri(config.url)
                    .header("Accept", "text/html")
                    .header("Accept-Encoding", "gzip, deflate")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .header("User-Agent", "Mozilla/5.0")
                    .header("Origin", "https://www.apps.miamioh.edu")
                    .header("Referer", "https://www.apps.miamioh.edu/courselist/")
                    .cookies { map -> cookies.forEach { (k, v) -> map.add(k, v) } }
                    .bodyValue(formBody)
                    .retrieve()
                    .toEntity(String::class.java)
                    .awaitSingle()
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            isBusy.set(true)
            logger.error("Request timed out connecting to Miami", e)
            throw ServerBusyException("Request timed out. Please try again later.")
        } catch (e: org.springframework.web.reactive.function.client.WebClientResponseException) {
            logger.warn("Received error status from Miami: {}", e.statusCode)
            ResponseEntity.status(e.statusCode).body(e.responseBodyAsString)
        } finally {
            if (activeRequests.decrementAndGet() == 0) isBusy.set(false)
        }
    }

    private fun determineRedirect(redirectUrl: String): String {
        return if (redirectUrl.startsWith("http")) redirectUrl
        else "https://www.apps.miamioh.edu${if (redirectUrl.startsWith("/")) redirectUrl else "/courselist/$redirectUrl"}"
    }

    private suspend fun getRedirectResponseHtml(redirectUrl: String): String {
        return webClient.get()
            .uri(determineRedirect(redirectUrl))
            .header("Referer", "https://www.apps.miamioh.edu/courselist/")
            .cookies { map -> cookies.forEach { (k, v) -> map.add(k, v) } }
            .retrieve()
            .bodyToMono(String::class.java)
            .awaitSingle()
    }
}
