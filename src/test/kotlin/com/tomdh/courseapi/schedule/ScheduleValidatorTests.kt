package com.tomdh.courseapi.schedule

import com.tomdh.courseapi.exceptions.types.QueryException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ScheduleValidatorTests {
    private val validator = ScheduleValidator()
    private val emptyFields = com.tomdh.courseapi.field.ValidFields(emptySet(), emptySet(), emptySet(), emptySet(), emptySet(), emptySet(), emptySet(), emptySet())

    @Test
    fun `validate ScheduleByCourseInput fails on missing courses`() {
        val input = ScheduleByCourseInput(school = "miami", term = "202410", campus = listOf("O"), courses = emptyList())
        assertThrows<IllegalArgumentException> { validator.validateScheduleFields(input, emptyFields) }
    }

    @Test
    fun `validate ScheduleByCourseInput fails on incorrectly formatted courses`() {
        val input = ScheduleByCourseInput(school = "miami", term = "202410", campus = listOf("O"), courses = listOf("CSE271"))
        assertThrows<IllegalArgumentException> { validator.validateScheduleFields(input, emptyFields) }
    }

    @Test
    fun `validate ScheduleByCourseInput succeeds on valid courses`() {
        // We must populate the validFields to match what is valid if we test success
        val validFields = com.tomdh.courseapi.field.ValidFields(
            terms = setOf("202410"),
            campuses = setOf("O"),
            subjects = emptySet(), deliveryTypes = emptySet(), levels = emptySet(), days = emptySet(), waitlistTypes = emptySet(), attributes = emptySet()
        )
        val input = ScheduleByCourseInput(school = "miami", term = "202410", campus = listOf("O"), courses = listOf("CSE 271"))
        validator.validateScheduleFields(input, validFields) // Should not throw
    }

    @Test
    fun `validate FillerByAttributesInput fails on missing attributes`() {
        val input = FillerByAttributesInput(school = "miami", term = "202410", campus = listOf("O"), courses = listOf("CSE 271"), attributes = emptyList())
        assertThrows<IllegalArgumentException> { validator.validateScheduleFields(input.toScheduleInput(), emptyFields, input.attributes) }
    }
}
