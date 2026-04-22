package com.tomdh.courseapi.field

sealed interface FieldResult
data class SuccessField(val fields: List<Field>) : FieldResult
data class ErrorField(val error: String, val message: String) : FieldResult

data class Field(val name: String)

data class ValidFields(
    val subjects: Set<String>,
    val campuses: Set<String>,
    val terms: Set<String>,
    val deliveryTypes: Set<String>,
    val levels: Set<String>,
    val days: Set<String>,
    val waitlistTypes: Set<String>,
    val attributes: Set<String>
)
