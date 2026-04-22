package com.tomdh.courseapi.school.miami

import com.tomdh.courseapi.course.Course
import com.tomdh.courseapi.course.CourseByCRNInput
import com.tomdh.courseapi.course.CourseByInfoInput
import com.tomdh.courseapi.exceptions.types.APIException
import com.tomdh.courseapi.exceptions.types.QueryException
import com.tomdh.courseapi.field.Field
import com.tomdh.courseapi.field.ValidFields
import com.tomdh.courseapi.school.SchoolConnector
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class MiamiConnector(
    private val client: MiamiClient,
    private val config: MiamiConfig
) : SchoolConnector {

    private val logger = LoggerFactory.getLogger(MiamiConnector::class.java)

    override val schoolId: String = "miami"

    @Volatile private var cachedValidFields: ValidFields? = null
    @Volatile private var fieldsCacheTimestamp: Long = 0
    private val fieldsCacheLock = Mutex()

    override suspend fun isAvailable(): Boolean {
        return try { client.getCourseList().isNotEmpty() } catch (e: Exception) {
            logger.warn("Miami availability check failed", e); false
        }
    }

    override suspend fun getCourseByInfo(input: CourseByInfoInput): List<Course> {
        val token = client.getOrFetchToken()
        if (token.isEmpty()) throw APIException("Empty Token")

        val formParts = ArrayList<String>(24)
        val formBody = buildFormRequest(formParts, input, token).joinToString("&")
        var resp = client.postResultResponse(formBody)

        val isExpired = resp.status == 419 || resp.body.contains("Page Expired", ignoreCase = true)
        if (isExpired) {
            val freshToken = client.forceFetchToken()
            if (freshToken.isNotEmpty()) {
                formParts[0] = "_token=${java.net.URLEncoder.encode(freshToken, java.nio.charset.StandardCharsets.UTF_8)}"
                resp = client.postResultResponse(formParts.joinToString("&"))
            }
        }

        if (resp.body.contains("Your query returned too many results.", ignoreCase = true)) {
            throw QueryException("Query returned too many results.")
        }

        return resp.body.parseMiamiCourses()
    }

    override suspend fun getCourseByCRN(input: CourseByCRNInput): List<Course> {
        return getCourseByInfo(CourseByInfoInput(crn = input.crn, term = input.term, campus = listOf("All")))
    }

    override suspend fun getOrFetchValidFields(): ValidFields = fieldsCacheLock.withLock {
        val now = System.currentTimeMillis()
        val cached = cachedValidFields
        if (cached != null && now - fieldsCacheTimestamp < config.fieldsCacheTimeoutMs) return cached

        val html = client.getCourseList()
        if (html.isEmpty()) throw APIException("Empty response when fetching valid fields")

        val fields = html.parseMiamiFields()
        cachedValidFields = fields
        fieldsCacheTimestamp = now
        fields
    }

    override suspend fun getTerms(): List<Field> {
        val html = client.getCourseList()
        if (html.isEmpty()) throw APIException("Empty terms")
        return html.parseMiamiTerms()
    }
}
