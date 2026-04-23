package com.tomdh.courseapi.schedule

import com.tomdh.courseapi.course.CanonicalTimeWindow
import com.tomdh.courseapi.course.SchedulableSection
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
class ScheduleResolverTests {

    @Mock lateinit var service: ScheduleService
    @InjectMocks lateinit var resolver: ScheduleResolver

    @Test
    fun `getSchedules returns SuccessSchedule`() = runBlocking {
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
        val expected = listOf(Schedule(sections = listOf(section), freeTime = 100))
        whenever(service.getSchedules(input)).thenReturn(expected)

        val result = resolver.getSchedules(input, limit = 10)

        assertTrue(result is SuccessSchedule)
        assertEquals(1, (result as SuccessSchedule).schedules.size)
    }

    @Test
    fun `getSchedules returns ErrorSchedule on failure`() = runBlocking {
        val input = ScheduleQueryInput(
            school = "miami",
            filters = mapOf("term" to "202410", "campus" to listOf("O")),
            courses = listOf("CSE 271")
        )
        whenever(service.getSchedules(input)).thenThrow(IllegalArgumentException("Invalid"))

        val result = resolver.getSchedules(input, limit = null)

        assertTrue(result is ErrorSchedule)
    }
}
