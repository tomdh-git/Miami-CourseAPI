package com.tomdh.courseapi.exceptions

import com.tomdh.courseapi.exceptions.types.*
import org.slf4j.Logger

fun Throwable.toErrorResponse(logger: Logger): Pair<String, String> {
    return when (this) {
        is IllegalArgumentException -> "VALIDATION_ERROR" to (message ?: "Invalid input")
        is QueryException -> "QUERY_ERROR" to (message ?: "Query error")
        is APIException -> "API_ERROR" to (message ?: "Upstream error")
        is ServerBusyException -> "SERVER_BUSY" to (message ?: "Server busy")
        else -> {
            logger.error("Unexpected error", this)
            "INTERNAL_ERROR" to "An unexpected error occurred"
        }
    }
}

inline fun <T> resolveQuery(
    logger: Logger,
    errorFactory: (String, String) -> T,
    action: () -> T
): T {
    return try {
        action()
    } catch (e: Exception) {
        val (code, msg) = e.toErrorResponse(logger)
        errorFactory(code, msg)
    }
}
