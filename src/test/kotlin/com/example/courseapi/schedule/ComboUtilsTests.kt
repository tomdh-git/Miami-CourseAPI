package com.example.courseapi.schedule

import com.example.courseapi.course.model.Course
import com.example.courseapi.schedule.utils.generateValidSchedules
import com.example.courseapi.schedule.utils.toMinutes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ComboUtilsTests {

    @Test
    fun `generateValidSchedules ignores conflicting combinations`() {
        // Arrange
        val c1_t1 = Course(crn = 111, title = "Course1", delivery = "MWF 10:00am-10:50am")
        val c1_t2 = Course(crn = 112, title = "Course1", delivery = "TR 10:00am-11:20am")
        val c2_t1 = Course(crn = 222, title = "Course2", delivery = "MWF 10:30am-11:20am") // Conflicts with c1_t1
        val c2_t2 = Course(crn = 223, title = "Course2", delivery = "MWF 01:00pm-01:50pm") // Non-conflicting

        val courseGroups = listOf(
            listOf(c1_t1, c1_t2), // Options for Course 1
            listOf(c2_t1, c2_t2)  // Options for Course 2
        )

        // Act
        val result = generateValidSchedules(
            courseGroups = courseGroups,
            preferredStartMin = 0,
            preferredEndMin = 1440,
            optimizeFreeTime = false,
            maxResults = 100
        )

        // Assert
        // Valid combos:
        // c1_t1 + c2_t2 
        // c1_t2 + c2_t1 
        // c1_t2 + c2_t2
        assertEquals(3, result.size)
        // Verify that c1_t1 and c2_t1 (which conflict) never appear in the same schedule
        val hasConflict = result.any { schedule ->
            val crns = schedule.courses.map { it.crn }
            crns.containsAll(listOf(111, 222))
        }
        assertEquals(false, hasConflict)
    }

    @Test
    fun `generateValidSchedules respects preferred time window`() {
        val c1_morning = Course(crn = 111, title = "Course1", delivery = "MWF 08:00am-08:50am")
        val c1_afternoon = Course(crn = 112, title = "Course1", delivery = "MWF 01:00pm-01:50pm")

        val result = generateValidSchedules(
            courseGroups = listOf(listOf(c1_morning, c1_afternoon)),
            preferredStartMin = toMinutes("12:00pm"), // Must start at noon or later
            preferredEndMin = 1440
        )

        assertEquals(1, result.size)
        assertEquals(112, result[0].courses[0].crn)
    }
}
