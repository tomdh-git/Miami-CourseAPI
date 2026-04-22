package com.tomdh.courseapi.schedule

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
    fun `getScheduleByCourses returns SuccessSchedule`() = runBlocking {
        val input = ScheduleByCourseInput(school = "miami", term = "202410", campus = listOf("O"), courses = listOf("CSE 271"))
        val expected = listOf(Schedule(courses = emptyList(), freeTime = 100))
        whenever(service.getScheduleByCourses(input)).thenReturn(expected)

        val result = resolver.getScheduleByCourses(input, limit = 10)
        
        assertTrue(result is SuccessSchedule)
        assertEquals(1, (result as SuccessSchedule).schedules.size)
    }

    @Test
    fun `getFillerByAttributes returns SuccessSchedule`() = runBlocking {
        val input = FillerByAttributesInput(school = "miami", term = "202410", campus = listOf("O"), courses = listOf("CSE 271"), attributes = listOf("PA1C"))
        val expected = listOf(Schedule(courses = emptyList(), freeTime = 50))
        whenever(service.getFillerByAttributes(input)).thenReturn(expected)

        val result = resolver.getFillerByAttributes(input, limit = null)
        
        assertTrue(result is SuccessSchedule)
    }
}
