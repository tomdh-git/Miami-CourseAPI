package com.tomdh.courseapi.exceptions.types

/**
 * Thrown when input validation fails. Carries ALL violations at once
 * so the user sees every issue in a single response.
 */
class ValidationException(
    val violations: List<String>
) : RuntimeException("Validation failed: ${violations.joinToString("; ")}")
