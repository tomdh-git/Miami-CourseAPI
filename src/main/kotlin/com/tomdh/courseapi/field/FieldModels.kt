package com.tomdh.courseapi.field

import com.tomdh.schoolconnector.field.Field

sealed interface FieldResult
data class SuccessField(val fields: List<Field>) : FieldResult
data class ErrorField(val error: String, val message: String) : FieldResult
