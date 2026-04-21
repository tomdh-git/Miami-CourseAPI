package com.example.courseapi.course

import com.example.courseapi.config.SchoolConnector
import com.example.courseapi.config.SchoolRegistry
import com.example.courseapi.course.api.CourseValidator
import com.example.courseapi.course.model.Course
import com.example.courseapi.course.model.input.CourseByCRNInput
import com.example.courseapi.course.service.CourseService
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

    @Mock
    lateinit var schoolRegistry: SchoolRegistry

    @Mock
    lateinit var schoolConnector: SchoolConnector

    @Mock
    lateinit var validator: CourseValidator

    @InjectMocks
    lateinit var courseService: CourseService

    @Test
    fun `getCourseByCRN returns course from mapped school`() = runBlocking {
        // Arrange
        val input = CourseByCRNInput(school = "miami", term = "202410", crn = 12345)
        val expectedCourse = Course(
            title = "Intro to CS",
            crn = 12345
        )

        whenever(schoolRegistry.getConnector("miami")).thenReturn(schoolConnector)
        whenever(schoolConnector.getCourseByCRN(any())).thenReturn(listOf(expectedCourse))

        // Act
        val result = courseService.getCourseByCRN(input)

        // Assert
        assertEquals(1, result.size)
        assertEquals(expectedCourse.title, result[0].title)
        assertEquals(expectedCourse.crn, result[0].crn)
    }
}
