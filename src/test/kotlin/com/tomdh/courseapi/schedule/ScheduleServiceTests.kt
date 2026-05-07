package com.tomdh.courseapi.schedule

import com.tomdh.courseapi.exceptions.types.ValidationException
import com.tomdh.schoolconnector.course.CanonicalTimeWindow
import com.tomdh.schoolconnector.course.SchedulableSection
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
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class ScheduleServiceTests {

    @Mock lateinit var schoolRegistry: SchoolRegistry
    @Mock lateinit var combinator: ScheduleCombinator
    @Mock lateinit var connector: SchoolConnector

    @InjectMocks lateinit var scheduleService: ScheduleService

    @Test
    fun `getSchedules validates and returns schedules`() = runBlocking {
        val input = ScheduleQueryInput(
            school = "miami",
            filters = mapOf("term" to "202410", "campus" to listOf("O")),
            courses = listOf("CSE 271")
        )
        val section = SchedulableSection(
            name = "CSE 271 - OOP",
            timeWindows = listOf(CanonicalTimeWindow("MONDAY", "10:00am", "10:50am")),
            data = mapOf("subject" to "CSE")
        )
        val expectedSchedule = Schedule(sections = listOf(section), freeTime = 100)

        whenever(schoolRegistry.getConnector("miami")).thenReturn(connector)
        whenever(connector.validateFilters(any())).thenReturn(emptyList())
        whenever(combinator.getScheduleByCourses(input, connector)).thenReturn(listOf(expectedSchedule))

        val result = scheduleService.getSchedules(input)

        assertEquals(1, result.size)
        assertEquals(100, result[0].freeTime)
    }

    @Test
    fun `getSchedules with fillerFilters delegates to filler combinator`() = runBlocking {
        val input = ScheduleQueryInput(
            school = "miami",
            filters = mapOf("term" to "202410", "campus" to listOf("O")),
            courses = listOf("CSE 271"),
            fillerFilters = mapOf("attributes" to listOf("PA1C"))
        )
        val section = SchedulableSection(
            name = "CSE 271 - OOP",
            timeWindows = listOf(CanonicalTimeWindow("MONDAY", "10:00am", "10:50am")),
            data = mapOf("subject" to "CSE")
        )
        val expectedSchedule = Schedule(sections = listOf(section), freeTime = 50)

        whenever(schoolRegistry.getConnector("miami")).thenReturn(connector)
        whenever(connector.validateFilters(any())).thenReturn(emptyList())
        whenever(combinator.getFillerSchedules(input, connector)).thenReturn(listOf(expectedSchedule))

        val result = scheduleService.getSchedules(input)

        assertEquals(1, result.size)
        assertEquals(50, result[0].freeTime)
    }

    @Test
    fun `getSchedules throws ValidationException when filters invalid`() = runBlocking {
        val input = ScheduleQueryInput(
            school = "miami",
            filters = mapOf("term" to ""),
            courses = listOf("CSE 271")
        )

        whenever(schoolRegistry.getConnector("miami")).thenReturn(connector)
        whenever(connector.validateFilters(any())).thenReturn(listOf("'term' is required"))

        assertThrows<ValidationException> {
            scheduleService.getSchedules(input)
        }
    }
}
