package com.tomdh.courseapi.exceptions

import com.tomdh.courseapi.exceptions.types.ValidationException
import com.tomdh.schoolconnector.exceptions.types.APIException
import com.tomdh.schoolconnector.exceptions.types.QueryException
import com.tomdh.schoolconnector.exceptions.types.ServerBusyException
import org.slf4j.Logger

fun Throwable.toErrorResponse(logger: Logger): Pair<String, String> =
    when (this) {
        is ValidationException -> "VALIDATION_ERROR" to violations.joinToString(" | ")
        is IllegalArgumentException -> "VALIDATION_ERROR" to (message ?: "Invalid input")
        is QueryException -> "QUERY_ERROR" to (message ?: "Query error")
        is APIException -> "API_ERROR" to (message ?: "Upstream error")
        is ServerBusyException -> "SERVER_BUSY" to (message ?: "Server busy")
        else -> {
            logger.error("Unexpected error", this)
            "INTERNAL_ERROR" to "An unexpected error occurred"
        }
    }

inline fun <T> resolveQuery(
    logger: Logger,
    errorFactory: (String, String) -> T,
    block: () -> T
): T =
    try {
        block()
    } catch (e: Exception) {
        val (code: String, msg: String) = e.toErrorResponse(logger)
        errorFactory(code, msg)
    }
