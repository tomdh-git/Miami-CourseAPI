package com.tomdh.courseapi.school

import com.tomdh.schoolconnector.school.SchoolConnector
import com.tomdh.schoolconnector.school.SchoolRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock

class SchoolRegistryTests {

    private lateinit var registry: SchoolRegistry
    private lateinit var mockConnector: SchoolConnector

    @BeforeEach
    fun setup() {
        mockConnector = mock()
        org.mockito.kotlin.whenever(mockConnector.schoolId).thenReturn("miami")
        // Provide mock list to constructor
        registry = SchoolRegistry(listOf(mockConnector))
    }

    @Test
    fun `getConnector returns matched connector`() {
        val result = registry.getConnector("miami")
        assertEquals(mockConnector, result)
    }

    @Test
    fun `getConnector throws IllegalArgumentException for unknown school`() {
        val e = assertThrows<IllegalArgumentException> {
            registry.getConnector("unknown")
        }
        assertEquals("Unknown school ID: unknown", e.message)
    }
}
