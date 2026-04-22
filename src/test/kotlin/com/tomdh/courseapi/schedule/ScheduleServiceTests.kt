package com.tomdh.courseapi.schedule

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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class ScheduleServiceTests {

    @Mock lateinit var schoolRegistry: SchoolRegistry
    @Mock lateinit var validator: ScheduleValidator
    @Mock lateinit var combinator: ScheduleCombinator
    @Mock lateinit var connector: SchoolConnector

    @InjectMocks lateinit var scheduleService: ScheduleService

    @Test
    fun `getScheduleByCourses validates and combines courses`() = runBlocking {
        val input = ScheduleByCourseInput(
            school = "miami",
            term = "202410",
            campus = listOf("O"),
            courses = listOf("CSE 271")
        )
        val expectedSchedule = Schedule(courses = emptyList<com.tomdh.courseapi.course.Course>(), freeTime = 100)
        val validFields = com.tomdh.courseapi.field.ValidFields(emptySet(), emptySet(), emptySet(), emptySet(), emptySet(), emptySet(), emptySet(), emptySet())

        whenever(schoolRegistry.getConnector("miami")).thenReturn(connector)
        whenever(connector.getOrFetchValidFields()).thenReturn(validFields)
        whenever(combinator.getScheduleByCourses(input, connector)).thenReturn(listOf(expectedSchedule))

        val result = scheduleService.getScheduleByCourses(input)

        assertEquals(1, result.size)
        assertEquals(100, result[0].freeTime)
        verify(validator).validateScheduleFields(input, validFields)
    }

    @Test
    fun `getFillerByAttributes validates and finds fillers`() = runBlocking {
        val input = FillerByAttributesInput(
            school = "miami",
            term = "202410",
            campus = listOf("O"),
            courses = listOf("CSE 271"),
            attributes = listOf("PA1C")
        )
        val expectedSchedule = Schedule(courses = emptyList<com.tomdh.courseapi.course.Course>(), freeTime = 50)
        val validFields = com.tomdh.courseapi.field.ValidFields(emptySet(), emptySet(), emptySet(), emptySet(), emptySet(), emptySet(), emptySet(), emptySet())

        whenever(schoolRegistry.getConnector("miami")).thenReturn(connector)
        whenever(connector.getOrFetchValidFields()).thenReturn(validFields)
        whenever(combinator.getFillerByAttributes(input, connector)).thenReturn(listOf(expectedSchedule))

        val result = scheduleService.getFillerByAttributes(input)

        assertEquals(1, result.size)
        assertEquals(50, result[0].freeTime)
        verify(validator).validateScheduleFields(input.toScheduleInput(), validFields, input.attributes)
    }
}
