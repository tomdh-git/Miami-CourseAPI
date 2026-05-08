package com.tomdh.courseapi.field

import com.tomdh.schoolconnector.exceptions.types.APIException
import com.tomdh.schoolconnector.field.Field
import com.tomdh.schoolconnector.school.SchoolConnector
import com.tomdh.schoolconnector.school.SchoolRegistry
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class FieldServiceTests {

    @Mock lateinit var registry: SchoolRegistry
    @Mock lateinit var connector: SchoolConnector

    @InjectMocks lateinit var fieldService: DefaultFieldService

    @Test
    fun `getTerms returns fields provided by connector`() = runBlocking {
        val expectedFields = listOf(Field(name = "202410"))
        whenever(registry.getConnector("miami")).thenReturn(connector)
        whenever(connector.getTerms()).thenReturn(expectedFields)

        val result = fieldService.getTerms("miami")
        
        assertEquals(1, result.size)
        assertEquals("202410", result[0].name)
    }

    @Test
    fun `getTerms throws APIException when connector returns empty list`() = runBlocking {
        whenever(registry.getConnector("miami")).thenReturn(connector)
        whenever(connector.getTerms()).thenReturn(emptyList())

        assertThrows<APIException> {
            fieldService.getTerms("miami")
        }
    }
}
