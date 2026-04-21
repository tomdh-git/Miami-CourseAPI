package com.tomdh.courseapi.school.miami.repo

import com.tomdh.courseapi.exceptions.APIException
import com.tomdh.courseapi.field.model.Field
import com.tomdh.courseapi.field.model.ValidFields
import com.tomdh.courseapi.school.miami.MiamiConfig
import com.tomdh.courseapi.school.miami.client.MiamiClient
import com.tomdh.courseapi.school.miami.parser.MiamiFieldParser
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
