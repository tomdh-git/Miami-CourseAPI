package com.tomdh.courseapi.course

import com.tomdh.courseapi.school.SchoolConnector
import com.tomdh.courseapi.school.SchoolRegistry
import com.tomdh.courseapi.course.api.CourseValidator
import com.tomdh.courseapi.course.model.Course
import com.tomdh.courseapi.course.model.input.CourseByCRNInput
import com.tomdh.courseapi.course.service.CourseService
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
