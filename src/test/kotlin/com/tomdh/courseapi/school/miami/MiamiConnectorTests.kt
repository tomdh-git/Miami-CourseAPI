package com.tomdh.courseapi.school.miami

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class MiamiConnectorTests {

    @Mock lateinit var client: MiamiClient
    @Mock lateinit var config: MiamiConfig

    @InjectMocks lateinit var connector: MiamiConnector

    @Test
    fun `validateFilters returns error for unknown keys`() = runBlocking {
        // Return dummy fields so validation doesn't fail on empty fields caching
        val dummyHtml = "<select id=\"termFilter\"><option value=\"202710\">Fall</option></select>"
        whenever(client.getCourseList()).thenReturn(dummyHtml)
        // Remove unnecessary stubbing for fieldsCacheTimeoutMs as getOrFetchValidFields handles cache hit checks which might skip if cached is null

        val filters = mapOf(
            "term" to "202710",
            "campus" to listOf("O"),
            "subject" to listOf("CSE"),
            "wrong" to "465",
            "invalid_key" to "test"
        )

        val errors = connector.validateFilters(filters)

        assertTrue(errors.any { it.contains("Unknown filter key: 'wrong'") })
        assertTrue(errors.any { it.contains("Unknown filter key: 'invalid_key'") })
    }

    @Test
    fun `validateFilters passes valid schema keys`() = runBlocking {
        // We still need valid mock options so that the rest of the parsing check doesn't emit other errors
        val dummyHtml = """
            <select id="termFilter"><option value="202710">Fall</option></select>
            <select id="campusFilter"><option value="O">Oxford</option></select>
            <select id="subject"><option value="CSE">CSE</option></select>
        """.trimIndent()
        whenever(client.getCourseList()).thenReturn(dummyHtml)

        val filters = mapOf(
            "term" to "202710",
            "campus" to listOf("O"),
            "subject" to listOf("CSE"),
            "courseNum" to "465"
        )

        val errors = connector.validateFilters(filters)

        // Expect no errors, meaning all keys represent valid Miami filters
        assertTrue(errors.isEmpty(), "Expected 0 errors but got: $errors")
    }
}
