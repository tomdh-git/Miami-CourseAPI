package com.tomdh.courseapi.course

import com.tomdh.courseapi.school.SchoolConnector
import com.tomdh.courseapi.school.SchoolRegistry
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class CourseServiceTests {

    @Mock lateinit var schoolRegistry: SchoolRegistry
    @Mock lateinit var schoolConnector: SchoolConnector
    @Mock lateinit var validator: CourseValidator

    @InjectMocks lateinit var courseService: CourseService

    @Test
    fun `getCourseByCRN returns course from mapped school`() = runBlocking {
        val input = CourseByCRNInput(school = "miami", term = "202410", crn = 12345)
        val expectedCourse = Course(title = "Intro to CS", crn = 12345)

        whenever(schoolRegistry.getConnector("miami")).thenReturn(schoolConnector)
        whenever(schoolConnector.getCourseByCRN(any())).thenReturn(listOf(expectedCourse))

        val result = courseService.getCourseByCRN(input)

        assertEquals(1, result.size)
        assertEquals(expectedCourse.title, result[0].title)
        assertEquals(expectedCourse.crn, result[0].crn)
    }

    @Test
    fun `getCourseByCRN throws QueryException when repository returns empty`() = runBlocking {
        val input = CourseByCRNInput(school = "miami", term = "202410", crn = 12345)
        
        whenever(schoolRegistry.getConnector("miami")).thenReturn(schoolConnector)
        whenever(schoolConnector.getCourseByCRN(any())).thenReturn(emptyList())

        org.junit.jupiter.api.assertThrows<com.tomdh.courseapi.exceptions.types.QueryException> {
            courseService.getCourseByCRN(input)
        }
    }

    @Test
    fun `getCourseByInfo triggers validation and returns courses`() = runBlocking {
        val input = CourseByInfoInput(school = "miami", term = "202410", campus = listOf("O"))
        val expectedCourse = Course(title = "Software Engineering")
        val validFields = com.tomdh.courseapi.field.ValidFields(emptySet(), emptySet(), emptySet(), emptySet(), emptySet(), emptySet(), emptySet(), emptySet())

        whenever(schoolRegistry.getConnector("miami")).thenReturn(schoolConnector)
        whenever(schoolConnector.getOrFetchValidFields()).thenReturn(validFields)
        whenever(schoolConnector.getCourseByInfo(any())).thenReturn(listOf(expectedCourse))

        val result = courseService.getCourseByInfo(input)
        assertEquals(1, result.size)
        assertEquals(expectedCourse.title, result[0].title)
        org.mockito.kotlin.verify(validator).validateCourseFields(input, validFields)
    }
}
