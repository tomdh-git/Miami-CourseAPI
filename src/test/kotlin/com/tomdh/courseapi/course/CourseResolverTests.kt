package com.tomdh.courseapi.course

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
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

    @Test
    fun `getCourseByInfo returns SuccessCourse on success`() = runBlocking {
        val input = CourseByInfoInput(school = "miami", term = "202410", campus = listOf("O"))
        val expectedCourses = listOf(Course(subject = "CSE", courseNum = "271", crn = 12345))
        whenever(service.getCourseByInfo(input)).thenReturn(expectedCourses)

        val result = resolver.getCourseByInfo(input, limit = 10)
        
        assertTrue(result is SuccessCourse)
        assertEquals(1, (result as SuccessCourse).courses.size)
    }

    @Test
    fun `getCourseByInfo returns ErrorCourse on failure`() = runBlocking {
        val input = CourseByInfoInput(school = "miami", term = "202410", campus = listOf("O"))
        whenever(service.getCourseByInfo(input)).thenThrow(IllegalArgumentException("Invalid term"))

        val result = resolver.getCourseByInfo(input, limit = null)
        
        assertTrue(result is ErrorCourse)
        assertEquals("VALIDATION_ERROR", (result as ErrorCourse).error)
    }

    @Test
    fun `getCourseByCRN returns SuccessCourse on success`() = runBlocking {
        val input = CourseByCRNInput(school = "miami", term = "202410", crn = 12345)
        val expectedCourses = listOf(Course(subject = "CSE", courseNum = "271", crn = 12345))
        whenever(service.getCourseByCRN(input)).thenReturn(expectedCourses)

        val result = resolver.getCourseByCRN(input)
        
        assertTrue(result is SuccessCourse)
    }
}
