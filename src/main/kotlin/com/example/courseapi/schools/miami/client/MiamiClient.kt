package com.example.courseapi.schools.miami

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
import com.example.courseapi.config.MiamiConfig
import org.slf4j.LoggerFactory

@Component
class MiamiClient(private val webClient: WebClient, private val config: MiamiConfig) {
    private val logger = LoggerFactory.getLogger(MiamiClient::class.java)
    @Volatile private var lastToken: String? = null
    @Volatile private var lastTokenTs: Long = 0
    private val refreshThreshold = 35_000L
    private val tokenLock = Mutex()
    @Volatile private var refreshJob: Job? = null
    private val refreshScope = CoroutineScope(Dispatchers.IO)
    @Volatile private var cachedHtml: String? = null
    @Volatile private var cachedHtmlTs: Long = 0
    private val htmlCacheLock = Mutex()
    private val tokenRegex = Regex("""<input[^>]*name="_token"[^>]*value="([^"]+)"""")
    
    data class HttpTextResponse(val status: Int, val body: String)

    private val cookies = java.util.concurrent.ConcurrentHashMap<String, String>()

    @PostConstruct
    fun warmUpConnection() {
        refreshScope.launch {
            try { getCourseList() }
            catch (e: Exception) { 
                logger.warn("Failed to warm up connection", e)
            }
        }
    }

    suspend fun getCourseList(): String {
        val now = System.currentTimeMillis()
        val cached = cachedHtml
        val age = now - cachedHtmlTs

        val requestFresh = cached != null && age < config.htmlCacheTimeoutMs
        if (requestFresh) return cached

        return htmlCacheLock.withLock {
            val againNow = System.currentTimeMillis()
            val againCached = cachedHtml
            val againAge = againNow - cachedHtmlTs

            val requestFreshAgain = againCached != null && againAge < config.htmlCacheTimeoutMs
            if (requestFreshAgain) {
                return againCached
            }
            
            logger.info("Fetching fresh course list from {}", config.url)
            val result = webClient.get()
                .uri(config.url)
                .header("Accept", "text/html")
                .header("User-Agent", "Mozilla/5.0")
                .exchangeToMono { response ->
                    response.cookies().forEach { (name, cookieList) ->
                        if (cookieList.isNotEmpty()) {
                            cookies[name] = cookieList[0].value
                        }
                    }
                    response.bodyToMono(String::class.java)
                }.awaitSingle()

            cachedHtml = result
            cachedHtmlTs = System.currentTimeMillis()
            result
        }
    }

    suspend fun getToken(): String {
        val html = getCourseList()
        return tokenRegex.find(html)?.groupValues?.get(1) ?: ""
    }

    suspend fun getOrFetchToken(): String {
        val now = System.currentTimeMillis()
        val cached = lastToken
        val age = now - lastTokenTs

        val inWindow = cached != null && age >= refreshThreshold && age < config.tokenTimeoutMs
        if (inWindow) {
            val currentJob = refreshJob
            val jobNotActive = currentJob == null || !currentJob.isActive
            if (jobNotActive) {
                refreshJob = refreshScope.launch {
                    tokenLock.withLock {
                        val freshToken = getToken()
                        lastToken = freshToken
                        lastTokenTs = System.currentTimeMillis()
                    }
                }
            }
            return cached
        }

        val requestFresh = cached != null && age < config.tokenTimeoutMs
        if (requestFresh) return cached

        return tokenLock.withLock {
            val againNow = System.currentTimeMillis()
            val lastTokenValid = lastToken != null && againNow - lastTokenTs < config.tokenTimeoutMs
            if (lastTokenValid) return lastToken!!

            val token = getToken()
            lastToken = token; lastTokenTs = againNow; token
        }
    }

    suspend fun postResultResponse(formBody: String): HttpTextResponse {
        val postResponse = getPostResponse(formBody)
        var resultHtml = postResponse.body ?: ""
        
        val hasRedirect = resultHtml.contains("meta http-equiv=\"refresh\"")
        if (hasRedirect) {
            val redirectUrl = """content=\s*"\s*\d+;\s*url='([^']+)'\s*""""
                .toRegex()
                .find(resultHtml)?.groupValues?.get(1)
            if (redirectUrl != null) {
                resultHtml = getRedirectResponseHtml(redirectUrl)
            }
        }
        return HttpTextResponse(
            postResponse.statusCode.value(),
            resultHtml
        )
    }

    private val activeRequests = java.util.concurrent.atomic.AtomicInteger(0)
    private val isBusy = java.util.concurrent.atomic.AtomicBoolean(false)

    private suspend fun getPostResponse(formBody: String): ResponseEntity<String> {
        if (isBusy.get()) {
            throw com.example.courseapi.exceptions.ServerBusyException("Server is busy, please try again later")
        }

        activeRequests.incrementAndGet()

        return try {
            kotlinx.coroutines.withTimeout(28_000L) {
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
            throw com.example.courseapi.exceptions.ServerBusyException("Request timed out. Please try again later.")
        } catch (e: org.springframework.web.reactive.function.client.WebClientResponseException) {
            logger.warn("Received error status from Miami: {}", e.statusCode)
            ResponseEntity.status(e.statusCode).body(e.responseBodyAsString)
        } finally {
            val noMoreRequests = activeRequests.decrementAndGet() == 0
            if (noMoreRequests) { isBusy.set(false) }
        }
    }

    private fun determineRedirect(redirectUrl: String): String {
        val redirectIsHttp = redirectUrl.startsWith("http")
        return if (redirectIsHttp) redirectUrl
        else {
            val isPath = redirectUrl.startsWith("/")
            val path = if (isPath) redirectUrl
            else "/courselist/$redirectUrl"
            "https://www.apps.miamioh.edu$path"
        }
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
