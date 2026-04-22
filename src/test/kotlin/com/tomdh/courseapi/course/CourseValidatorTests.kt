package com.tomdh.courseapi.course

import com.tomdh.courseapi.exceptions.types.QueryException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CourseValidatorTests {
    private val validator = CourseValidator()
    private val emptyFields = com.tomdh.courseapi.field.ValidFields(emptySet(), emptySet(), emptySet(), emptySet(), emptySet(), emptySet(), emptySet(), emptySet())

    @Test
    fun `validate CourseByInfoInput throws IllegalArgumentException without school`() {
        val input = CourseByInfoInput(school = "", term = "202410", campus = listOf("O"))
        assertThrows<IllegalArgumentException> { validator.validateCourseFields(input, emptyFields) }
    }

    @Test
    fun `validate CourseByInfoInput throws IllegalArgumentException without term`() {
        val input = CourseByInfoInput(school = "miami", term = "", campus = listOf("O"))
        assertThrows<IllegalArgumentException> { validator.validateCourseFields(input, emptyFields) }
    }

    @Test
    fun `validate CourseByInfoInput throws IllegalArgumentException without campus`() {
        val input = CourseByInfoInput(school = "miami", term = "202410", campus = emptyList())
        assertThrows<IllegalArgumentException> { validator.validateCourseFields(input, emptyFields) }
    }

    @Test
    fun `validate CourseByInfoInput succeeds with valid data`() {
        val validFields = com.tomdh.courseapi.field.ValidFields(
            terms = setOf("202410"), campuses = setOf("O"),
            subjects = emptySet(), deliveryTypes = emptySet(), levels = emptySet(), days = emptySet(), waitlistTypes = emptySet(), attributes = emptySet()
        )
        val input = CourseByInfoInput(school = "miami", term = "202410", campus = listOf("O"))
        validator.validateCourseFields(input, validFields) // Should not throw
    }
}
