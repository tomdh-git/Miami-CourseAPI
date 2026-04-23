package com.tomdh.courseapi.course

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*

@ExtendWith(MockitoExtension::class)
class CourseResolverTests {

    @Mock lateinit var service: CourseService
    @InjectMocks lateinit var resolver: CourseResolver

    private fun testSection(name: String = "CSE 271 - OOP") = SchedulableSection(
        name = name,
        timeWindows = listOf(CanonicalTimeWindow("MONDAY", "10:00am", "10:50am")),
        data = mapOf("subject" to "CSE", "courseNum" to "271")
    )

    @Test
    fun `getCourses returns SuccessCourse on success`() = runBlocking {
        val input = CourseQueryInput(school = "miami", filters = mapOf("term" to "202410", "campus" to listOf("O")))
        whenever(service.getCourses("miami", input.filters, 10)).thenReturn(listOf(testSection()))

        val result = resolver.getCourses(input, limit = 10)

        assertTrue(result is SuccessCourse)
        assertEquals(1, (result as SuccessCourse).courses.size)
    }

    @Test
    fun `getCourses returns ErrorCourse on failure`() = runBlocking {
        val input = CourseQueryInput(school = "miami", filters = mapOf("term" to "202410", "campus" to listOf("O")))
        whenever(service.getCourses(eq("miami"), any(), eq(100))).thenThrow(IllegalArgumentException("Invalid term"))

        val result = resolver.getCourses(input, limit = null)

        assertTrue(result is ErrorCourse)
        assertEquals("VALIDATION_ERROR", (result as ErrorCourse).error)
    }
}
