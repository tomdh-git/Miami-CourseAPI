package com.tomdh.courseapi.field

sealed interface FieldResult
data class SuccessField(val fields: List<Field>) : FieldResult
data class ErrorField(val error: String, val message: String) : FieldResult

data class Field(val name: String)
