package com.tomdh.courseapi.datafetchers

import com.tomdh.courseapi.generated.types.ErrorSchoolSchema
import com.tomdh.courseapi.generated.types.SuccessSchoolSchema
import com.tomdh.schoolconnector.school.SchoolConnector
import com.tomdh.schoolconnector.school.SchoolRegistry
import com.tomdh.schoolconnector.school.SchoolSchema
import com.tomdh.schoolconnector.exceptions.types.APIException
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class SchoolSchemaDataFetcherTests {

    @Mock lateinit var registry: SchoolRegistry
    @Mock lateinit var connector: SchoolConnector

    private val resolver by lazy { SchoolSchemaDataFetcher(registry) }

    private val testInputSchema = mapOf(
        "campus" to mapOf("type" to "array", "required" to true),
        "term" to mapOf("type" to "string", "required" to true)
    )
    private val testOutputSchema = mapOf(
        "crn" to mapOf("type" to "integer"),
        "subject" to mapOf("type" to "string")
    )

    @Test
    fun `getSchoolSchema returns SuccessSchoolSchema with input and output schemas`() {
        whenever(registry.getConnector("miami")).thenReturn(connector)
        whenever(connector.getSchema()).thenReturn(SchoolSchema(testInputSchema, testOutputSchema))

        val result = resolver.getSchoolSchema("miami")

        assertTrue(result is SuccessSchoolSchema)
        val success = result as SuccessSchoolSchema
        assertEquals("miami", success.school)
        assertEquals(testInputSchema, success.inputSchema)
        assertEquals(testOutputSchema, success.outputSchema)
    }

    @Test
    fun `getSchoolSchema returns ErrorSchoolSchema for invalid school`() {
        whenever(registry.getConnector("INVALID")).thenThrow(IllegalArgumentException("Unknown school: INVALID"))

        val result = resolver.getSchoolSchema("INVALID")

        assertTrue(result is ErrorSchoolSchema)
        assertEquals("VALIDATION_ERROR", (result as ErrorSchoolSchema).error)
    }

    @Test
    fun `getSchoolSchema returns ErrorSchoolSchema on API failure`() {
        whenever(registry.getConnector("miami")).thenReturn(connector)
        whenever(connector.getSchema()).thenThrow(APIException("Schema endpoint unreachable"))

        val result = resolver.getSchoolSchema("miami")

        assertTrue(result is ErrorSchoolSchema)
        assertEquals("API_ERROR", (result as ErrorSchoolSchema).error)
    }
}
