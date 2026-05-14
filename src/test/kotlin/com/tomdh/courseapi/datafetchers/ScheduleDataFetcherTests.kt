package com.tomdh.courseapi.datafetchers

import com.tomdh.courseapi.config.CourseApiProperties
import com.tomdh.courseapi.generated.types.ErrorSchedule
import com.tomdh.courseapi.generated.types.Schedule
import com.tomdh.courseapi.generated.types.ScheduleQueryInput
import com.tomdh.courseapi.generated.types.SuccessSchedule
import com.tomdh.schoolconnector.course.CanonicalTimeWindow
import com.tomdh.schoolconnector.course.SchedulableSection
import com.tomdh.schoolconnector.exceptions.types.APIException
import com.tomdh.schoolconnector.exceptions.types.QueryException
import com.tomdh.courseapi.service.ScheduleService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever

@Suppress("UNCHECKED_CAST")
@ExtendWith(MockitoExtension::class)
class ScheduleDataFetcherTests {

    @Mock lateinit var service: ScheduleService
    private val properties = CourseApiProperties()

    private val resolver by lazy { ScheduleDataFetcher(service, properties) }

    private fun filters(vararg pairs: Pair<String, Any?>): Object =
        mapOf(*pairs) as Object

    @Test
    fun `getSchedules returns SuccessSchedule`() = runBlocking {
        val input = ScheduleQueryInput(
            school = "miami",
            filters = filters("term" to "202410", "campus" to listOf("O")),
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
    fun `getSchedules returns ErrorSchedule for validation`() = runBlocking {
        val input = ScheduleQueryInput(
            school = "miami",
            filters = filters("term" to "202410", "campus" to listOf("O")),
            courses = listOf("CSE 271")
        )
        whenever(service.getSchedules(input)).thenThrow(com.tomdh.courseapi.exceptions.types.ValidationException(listOf("Invalid mapping constraint")))

        val result = resolver.getSchedules(input, limit = null)

        assertTrue(result is ErrorSchedule)
        assertEquals("VALIDATION_ERROR", (result as ErrorSchedule).error)
    }

    @Test
    fun `getSchedules returns ErrorSchedule for query execution`() = runBlocking {
        val input = ScheduleQueryInput(
            school = "miami",
            filters = filters("term" to "202410"),
            courses = listOf()
        )
        whenever(service.getSchedules(input)).thenThrow(QueryException("Missing combination paths generated"))

        val result = resolver.getSchedules(input, limit = null)

        assertTrue(result is ErrorSchedule)
        assertEquals("QUERY_ERROR", (result as ErrorSchedule).error)
    }

    @Test
    fun `getSchedules returns ErrorSchedule for api parsing logic outage`() = runBlocking {
        val input = ScheduleQueryInput(
            school = "miami",
            filters = filters("term" to "202410"),
            courses = listOf("CSE 271")
        )
        whenever(service.getSchedules(input)).thenThrow(APIException("Connector API failure logic timed out upstream"))

        val result = resolver.getSchedules(input, limit = null)

        assertTrue(result is ErrorSchedule)
        assertEquals("API_ERROR", (result as ErrorSchedule).error)
    }

    @Test
    fun `getSchedules returns ErrorSchedule for server busy`() = runBlocking {
        val input = ScheduleQueryInput(
            school = "miami",
            filters = filters("term" to "202410", "campus" to listOf("O")),
            courses = listOf("CSE 271")
        )
        whenever(service.getSchedules(input)).thenThrow(com.tomdh.schoolconnector.exceptions.types.ServerBusyException("Rate limited"))

        val result = resolver.getSchedules(input, limit = null)

        assertTrue(result is ErrorSchedule)
        assertEquals("SERVER_BUSY", (result as ErrorSchedule).error)
    }

    @Test
    fun `getSchedules respects limit parameter`() = runBlocking {
        val input = ScheduleQueryInput(
            school = "miami",
            filters = filters("term" to "202410", "campus" to listOf("O")),
            courses = listOf("CSE 271")
        )
        val section = SchedulableSection(
            name = "CSE 271 - OOP",
            timeWindows = listOf(CanonicalTimeWindow("MONDAY", "10:00am", "10:50am")),
            data = mapOf("subject" to "CSE")
        )
        val schedules = (1..5).map { Schedule(sections = listOf(section), freeTime = it * 10) }
        whenever(service.getSchedules(input)).thenReturn(schedules)

        val result = resolver.getSchedules(input, limit = 2)

        assertTrue(result is SuccessSchedule)
        assertEquals(2, (result as SuccessSchedule).schedules.size)
    }
}

