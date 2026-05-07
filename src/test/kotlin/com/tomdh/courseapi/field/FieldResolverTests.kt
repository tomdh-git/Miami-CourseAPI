package com.tomdh.courseapi.field

import com.tomdh.schoolconnector.field.Field
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class FieldResolverTests {

    @Mock lateinit var service: FieldService
    @InjectMocks lateinit var resolver: FieldResolver

    @Test
    fun `getTerms returns SuccessField`() = runBlocking {
        val expected = listOf(Field(name = "202410"))
        whenever(service.getTerms("miami")).thenReturn(expected)

        val result = resolver.getTerms("miami")
        
        assertTrue(result is SuccessField)
        assertEquals(1, (result as SuccessField).fields.size)
    }

    @Test
    fun `getTerms returns ErrorField on failure`() = runBlocking {
        whenever(service.getTerms("unknown")).thenThrow(IllegalArgumentException("Unknown school"))

        val result = resolver.getTerms("unknown")
        
        assertTrue(result is ErrorField)
        assertEquals("VALIDATION_ERROR", (result as ErrorField).error)
    }
}
