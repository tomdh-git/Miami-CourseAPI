package com.tomdh.courseapi.school.miami

import com.tomdh.courseapi.exceptions.types.APIException
import com.tomdh.courseapi.exceptions.types.QueryException
import com.tomdh.courseapi.school.SchoolSchema
import kotlinx.coroutines.sync.withLock

@org.springframework.stereotype.Component
class MiamiConnector(
    private val client: MiamiClient,
    private val config: MiamiConfig
) : com.tomdh.courseapi.school.SchoolConnector {

    private val logger = org.slf4j.LoggerFactory.getLogger(MiamiConnector::class.java)

    override val schoolId: String = "miami"

    @Volatile private var cachedValidFields: MiamiValidFields? = null
    @Volatile private var fieldsCacheTimestamp: Long = 0
    private val fieldsCacheLock = kotlinx.coroutines.sync.Mutex()

    override suspend fun isAvailable(): Boolean {
        return try { client.getCourseList().isNotEmpty() }
        catch (e: Exception) {
            logger.warn("Miami availability check failed", e)
            false
        }
    }

    override suspend fun queryCourses(filters: Map<String, Any?>): List<com.tomdh.courseapi.course.SchedulableSection> {
        val token = client.getOrFetchToken()
        if (token.isEmpty()) throw APIException("Empty Token")

        val formParts = ArrayList<String>(24)
        val formBody = buildFormRequest(formParts, filters, token).joinToString("&")
        var resp = client.postResultResponse(formBody)

        val isExpired = resp.status == 419 || resp.body.contains("Page Expired", ignoreCase = true)
        if (isExpired) {
            val freshToken = client.forceFetchToken()
            if (freshToken.isNotEmpty()) {
                formParts[0] = "_token=${
                    java.net.URLEncoder.encode(
                        freshToken, 
                        java.nio.charset.StandardCharsets.UTF_8)
                }"
                resp = client.postResultResponse(formParts.joinToString("&"))
            }
        }

        if (resp.body.contains("Your query returned too many results.", ignoreCase = true)) {
            throw QueryException("Query returned too many results.")
        }

        return resp.body.parseMiamiCoursesToSections()
    }

    override suspend fun validateFilters(filters: Map<String, Any?>): List<String> {
        val errors = mutableListOf<String>()
        val fields = getOrFetchValidFields()

        // Check for unknown filter keys
        val allowedKeys = getSchema().inputSchema.keys
        for (key in filters.keys) {
            if (key !in allowedKeys) {
                errors.add("Unknown filter key: '$key'")
            }
        }

        // Term — required
        val term = filters["term"] as? String
        if (term.isNullOrEmpty()) {
            errors.add("'term' is required")
        } else if (term !in fields.terms) {
            errors.add("'term' value '$term' is not valid")
        }

        // Campus — required
        val campus = filters.asStringListSafe("campus")
        if (campus.isEmpty()) {
            errors.add("'campus' is required and must be a non-empty array")
        } else if (!campus.all { it in fields.campuses }) {
            errors.add("'campus' contains invalid values")
        }

        // Subject — optional but must be valid if provided
        val subjects = filters.asStringListSafe("subject")
        if (subjects.isNotEmpty() && !subjects.all { it in fields.subjects }) {
            errors.add("'subject' contains invalid values")
        }

        // CourseNum requires exactly one subject
        val courseNum = filters["courseNum"] as? String
        if (!courseNum.isNullOrEmpty()) {
            if (subjects.isEmpty()) errors.add("'courseNum' requires a 'subject' to be specified")
            if (subjects.size > 1) errors.add("'courseNum' requires exactly one 'subject', got ${subjects.size}")
        }

        // Delivery — optional but must be valid
        val delivery = filters.asStringListSafe("delivery")
        if (delivery.isNotEmpty() && !delivery.all { it in fields.deliveryTypes }) {
            errors.add("'delivery' contains invalid values")
        }

        // Attributes — optional but must be valid
        val attributes = filters.asStringListSafe("attributes")
        if (attributes.isNotEmpty() && !attributes.all { it in fields.attributes }) {
            errors.add("'attributes' contains invalid values")
        }

        // OpenWaitlist — optional
        val openWaitlist = filters["openWaitlist"] as? String
        if (!openWaitlist.isNullOrEmpty() && openWaitlist !in fields.waitlistTypes) {
            errors.add("'openWaitlist' value '$openWaitlist' is not valid")
        }

        // Level — optional
        val level = filters["level"] as? String
        if (!level.isNullOrEmpty() && level !in fields.levels) {
            errors.add("'level' value '$level' is not valid")
        }

        // Days filter — optional
        val daysFilter = filters.asStringListSafe("daysFilter")
        if (daysFilter.isNotEmpty() && !daysFilter.all { it in fields.days }) {
            errors.add("'daysFilter' contains invalid values")
        }

        // StartEndTime — must be a pair if provided
        val startEndTime = filters.asStringListSafe("startEndTime")
        if (startEndTime.isNotEmpty() && startEndTime.size != 2) {
            errors.add("'startEndTime' must contain exactly 2 values (start and end)")
        }

        return errors
    }

    override fun getSchema(): SchoolSchema = SchoolSchema(
        inputSchema = mapOf(
            "term" to mapOf("type" to "string", "required" to true, "description" to "Academic term code (e.g. '202510')"),
            "campus" to mapOf("type" to "array<string>", "required" to true, "description" to "Campus codes (e.g. ['O', 'H'])"),
            "subject" to mapOf("type" to "array<string>", "required" to false, "description" to "Subject codes (e.g. ['CSE', 'MTH'])"),
            "courseNum" to mapOf("type" to "string", "required" to false, "description" to "Course number (requires exactly one subject)"),
            "crn" to mapOf("type" to "int", "required" to false, "description" to "Course Reference Number"),
            "delivery" to mapOf("type" to "array<string>", "required" to false, "description" to "Delivery methods (e.g. ['Face2Face'])"),
            "attributes" to mapOf("type" to "array<string>", "required" to false, "description" to "Course attributes (e.g. ['PA1C'])"),
            "openWaitlist" to mapOf("type" to "string", "required" to false, "description" to "Waitlist filter (e.g. 'open')"),
            "level" to mapOf("type" to "string", "required" to false, "description" to "Course level (e.g. 'UG', 'GR')"),
            "courseTitle" to mapOf("type" to "string", "required" to false, "description" to "Keywords in the course title"),
            "daysFilter" to mapOf("type" to "array<string>", "required" to false, "description" to "Days of the week (e.g. ['M', 'W', 'F'])"),
            "creditHours" to mapOf("type" to "int", "required" to false, "description" to "Exact credit hours"),
            "startEndTime" to mapOf("type" to "array<string>", "required" to false, "description" to "Time range filter (exactly 2 values)"),
            "partOfTerm" to mapOf("type" to "array<string>", "required" to false, "description" to "Part of term filter")
        ),
        outputSchema = mapOf(
            "subject" to mapOf("type" to "string", "description" to "Subject code"),
            "courseNum" to mapOf("type" to "string", "description" to "Course number"),
            "title" to mapOf("type" to "string", "description" to "Course title"),
            "section" to mapOf("type" to "string", "description" to "Section identifier"),
            "crn" to mapOf("type" to "int", "description" to "Course Reference Number"),
            "campus" to mapOf("type" to "string", "description" to "Campus code"),
            "credits" to mapOf("type" to "int", "description" to "Credit hours"),
            "capacity" to mapOf("type" to "string", "description" to "Section capacity"),
            "requests" to mapOf("type" to "string", "description" to "Enrollment requests"),
            "delivery" to mapOf("type" to "string", "description" to "Delivery/time info (e.g. 'MWF 10:00am-10:50am 01/13-05/02')")
        )
    )

    override suspend fun getTerms(): List<com.tomdh.courseapi.field.Field> {
        val html = client.getCourseList()
        if (html.isEmpty()) throw APIException("Empty terms")
        return html.parseMiamiTerms()
    }

    internal suspend fun getOrFetchValidFields(): MiamiValidFields = fieldsCacheLock.withLock {
        val now = System.currentTimeMillis()
        val cached = cachedValidFields
        if (cached != null && now - fieldsCacheTimestamp < config.fieldsCacheTimeoutMs) return cached

        val html = client.getCourseList()
        if (html.isEmpty()) throw APIException("Empty response when fetching valid fields")

        val miamiFields = html.parseMiamiFields()
        cachedValidFields = miamiFields
        fieldsCacheTimestamp = now
        miamiFields
    }

    private fun Map<String, Any?>.asStringListSafe(key: String): List<String> {
        val value = this[key] ?: return emptyList()
        return when (value) {
            is List<*> -> value
                .filterNotNull()
                .map { it.toString() }
            is String -> listOf(value)
            else -> emptyList()
        }
    }
}
