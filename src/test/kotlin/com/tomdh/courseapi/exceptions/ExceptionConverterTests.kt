package com.tomdh.courseapi.exceptions

import com.tomdh.courseapi.exceptions.types.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.slf4j.Logger

class ExceptionConverterTests {

    private val logger = mock<Logger>()

    @Test
    fun `toErrorResponse maps QueryException to QUERY_ERROR`() {
        val ex = QueryException("Bad query")
        val (code, msg) = ex.toErrorResponse(logger)
        assertEquals("QUERY_ERROR", code)
        assertEquals("Bad query", msg)
    }

    @Test
    fun `toErrorResponse maps ServerBusyException to SERVER_BUSY`() {
        val ex = ServerBusyException("Busy")
        val (code, msg) = ex.toErrorResponse(logger)
        assertEquals("SERVER_BUSY", code)
        assertEquals("Busy", msg)
    }

    @Test
    fun `toErrorResponse maps APIException to API_ERROR`() {
        val ex = APIException("Failed")
        val (code, msg) = ex.toErrorResponse(logger)
        assertEquals("API_ERROR", code)
        assertEquals("Failed", msg)
    }

    @Test
    fun `resolveQuery catches exceptions and invokes errorFactory`() {
        val ex = APIException("Failed")
        val result = resolveQuery(logger, { code, msg -> "Error: $code" }) {
            throw ex
        }
        assertEquals("Error: API_ERROR", result)
    }
}
