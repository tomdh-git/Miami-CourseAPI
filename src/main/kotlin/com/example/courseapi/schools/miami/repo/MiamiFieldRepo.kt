package com.example.courseapi.schools.miami.repo

import com.example.courseapi.exceptions.APIException
import com.example.courseapi.field.model.Field
import com.example.courseapi.field.model.ValidFields
import com.example.courseapi.schools.miami.MiamiConfig
import com.example.courseapi.schools.miami.client.MiamiClient
import com.example.courseapi.schools.miami.parser.MiamiFieldParser
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.stereotype.Repository

@Repository
class MiamiFieldRepo(
    private val requests: MiamiClient,
    private val parser: MiamiFieldParser,
    private val config: MiamiConfig
) {
    @Volatile private var cachedValidFields: ValidFields? = null
    @Volatile private var cacheTimestamp: Long = 0
    private val fieldsCacheLock = Mutex()

    suspend fun getTerms(): List<Field> {
        val termsRaw = requests.getCourseList()
        if (termsRaw.isEmpty()) throw APIException("Empty terms")
        return parser.parseTerms(termsRaw)
    }

    suspend fun getOrFetchValidFields(): ValidFields = fieldsCacheLock.withLock {
        val now = System.currentTimeMillis()
        val cached = cachedValidFields

        val isFresh = cached != null && now - cacheTimestamp < config.fieldsCacheTimeoutMs
        if (isFresh) return cached

        val html = requests.getCourseList()
        if (html.isEmpty()) throw APIException("Empty response when fetching valid fields")

        val fields = parser.parseAllFields(html)
        cachedValidFields = fields
        cacheTimestamp = now
        fields
    }
}
