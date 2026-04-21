package com.example.courseapi.field.model

sealed interface FieldResult
data class SuccessField(val fields: List<Field>) : FieldResult
data class ErrorField(val error: String, val message: String) : FieldResult
