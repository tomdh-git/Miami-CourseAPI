package com.tomdh.courseapi.school

sealed interface SchoolSchemaResult
data class SuccessSchoolSchema(
    val school: String,
    val inputSchema: Map<String, Any?>,
    val outputSchema: Map<String, Any?>
) : SchoolSchemaResult

data class ErrorSchoolSchema(
    val error: String,
    val message: String
) : SchoolSchemaResult
