package com.tomdh.courseapi.service

import com.tomdh.courseapi.config.CourseApiProperties
import com.tomdh.courseapi.exceptions.types.ValidationException
import com.tomdh.schoolconnector.course.CanonicalTimeWindow
import com.tomdh.schoolconnector.course.SchedulableSection
import com.tomdh.schoolconnector.exceptions.types.QueryException
import com.tomdh.schoolconnector.school.SchoolConnector
import com.tomdh.schoolconnector.school.SchoolRegistry
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class CourseServiceTests {

  @Mock lateinit var schoolRegistry: SchoolRegistry
  @Mock lateinit var schoolConnector: SchoolConnector
  private val properties = CourseApiProperties()

  private val courseService by lazy { DefaultCourseService(schoolRegistry) }

  private fun testSection(name: String = "CSE 271 - OOP") = SchedulableSection(
    name = name,
    timeWindows = listOf(CanonicalTimeWindow("MONDAY", "10:00am", "10:50am")),
    data = mapOf("subject" to "CSE", "courseNum" to "271", "crn" to 12345)
  )

  @Test
  fun `getCourses returns sections from connector`() = runBlocking {
    val filters = mapOf<String, Any?>("term" to "202410", "campus" to listOf("O"))

    whenever(schoolRegistry.getConnector("miami")).thenReturn(schoolConnector)
    whenever(schoolConnector.validateFilters(any())).thenReturn(emptyList())
    whenever(schoolConnector.queryCourses(any())).thenReturn(listOf(testSection()))

    val result = courseService.getCourses("miami", filters, 100)

    assertEquals(1, result.size)
    assertEquals("CSE 271 - OOP", result[0].name)
  }

  @Test
  fun `getCourses throws QueryException when connector returns empty`() = runBlocking {
    val filters = mapOf<String, Any?>("term" to "202410", "campus" to listOf("O"))

    whenever(schoolRegistry.getConnector("miami")).thenReturn(schoolConnector)
    whenever(schoolConnector.validateFilters(any())).thenReturn(emptyList())
    whenever(schoolConnector.queryCourses(any())).thenReturn(emptyList())

    assertThrows<QueryException> {
      courseService.getCourses("miami", filters, 100)
    }
  }

  @Test
  fun `getCourses throws ValidationException when validation fails`() = runBlocking {
    val filters = mapOf<String, Any?>("term" to "", "campus" to listOf("O"))

    whenever(schoolRegistry.getConnector("miami")).thenReturn(schoolConnector)
    whenever(schoolConnector.validateFilters(any())).thenReturn(listOf("'term' is required", "'campus' invalid"))

    val ex = assertThrows<ValidationException> {
      courseService.getCourses("miami", filters, 100)
    }
    assertEquals(2, ex.violations.size)
  }

  @Test
  fun `getCourses truncates results to limit`() = runBlocking {
    val filters = mapOf<String, Any?>("term" to "202410", "campus" to listOf("O"))
    val sections = (1..10).map { testSection("CSE $it - Test Course") }

    whenever(schoolRegistry.getConnector("miami")).thenReturn(schoolConnector)
    whenever(schoolConnector.validateFilters(any())).thenReturn(emptyList())
    whenever(schoolConnector.queryCourses(any())).thenReturn(sections)

    val result = courseService.getCourses("miami", filters, 3)

    assertEquals(3, result.size)
  }

  @Test
  fun `getCourses throws IllegalArgumentException for unknown school`() = runBlocking {
    val filters = mapOf<String, Any?>("term" to "202410")

    whenever(schoolRegistry.getConnector("INVALID")).thenThrow(IllegalArgumentException("Unknown school: INVALID"))

    assertThrows<IllegalArgumentException> {
      courseService.getCourses("INVALID", filters, 100)
    }
  }
}

