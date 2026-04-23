package com.tomdh.courseapi.school

import com.tomdh.courseapi.course.SchedulableSection

interface SchoolConnector {
    val schoolId: String

    fun supportsScheduling(): Boolean = true

    suspend fun isAvailable(): Boolean

    /**
     * Query courses using school-specific filters.
     * Each connector maps the raw JSON filter map to its own query format,
     * then returns results as [SchedulableSection]s with canonical fields + raw data.
     */
    suspend fun queryCourses(filters: Map<String, Any?>): List<SchedulableSection>

    /**
     * Validate input filters and return a list of ALL validation errors.
     * Empty list = valid input. This replaces the old standalone validators.
     */
    suspend fun validateFilters(filters: Map<String, Any?>): List<String>

    /**
     * Describe the input/output schema for this school.
     * Used by the getSchoolSchema introspection query.
     */
    fun getSchema(): SchoolSchema

    /**
     * Get available terms/periods for this school.
     */
    suspend fun getTerms(): List<com.tomdh.courseapi.field.Field>
}

/**
 * Describes what filter keys a school accepts (inputSchema)
 * and what fields its course output contains (outputSchema).
 */
data class SchoolSchema(
    val inputSchema: Map<String, Any?>,
    val outputSchema: Map<String, Any?>
)
