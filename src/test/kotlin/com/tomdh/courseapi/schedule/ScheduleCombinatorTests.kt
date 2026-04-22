package com.tomdh.courseapi.schedule

import com.tomdh.courseapi.course.Course
import com.tomdh.courseapi.exceptions.types.QueryException
import com.tomdh.courseapi.school.SchoolConnector
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class ScheduleCombinatorTests {

    @Mock lateinit var cache: FillerAttributeCache
    @Mock lateinit var connector: SchoolConnector

    @InjectMocks lateinit var combinator: ScheduleCombinator

    @Test
    fun `getScheduleByCourses correctly combines courses and filters by time`() = runBlocking {
        val input = ScheduleByCourseInput(
            school = "miami",
            term = "202410",
            campus = listOf("O"),
            courses = listOf("CSE 271", "MTH 251"),
            preferredStart = "9:00am",
            preferredEnd = "5:00pm",
            optimizeFreeTime = true
        )

        val cseCourse = Course(
            subject = "CSE", courseNum = "271", crn = 1,
            delivery = "MWF 10:05 am-11:00 am"
        )
        val mthCourse = Course(
            subject = "MTH", courseNum = "251", crn = 2,
            delivery = "MWF 11:40 am-12:35 pm"
        )

        // Mock connector to return these courses
        whenever(connector.getCourseByInfo(any<com.tomdh.courseapi.course.CourseByInfoInput>())).thenAnswer { invocation ->
            val req = invocation.arguments[0] as com.tomdh.courseapi.course.CourseByInfoInput
            when {
                req.subject?.contains("CSE") == true -> listOf(cseCourse)
                req.subject?.contains("MTH") == true -> listOf(mthCourse)
                else -> emptyList<Course>()
            }
        }

        val result = combinator.getScheduleByCourses(input, connector)
        
        assertEquals(1, result.size)
        assertEquals(2, result[0].courses.size)
    }

    @Test
    fun `getScheduleByCourses throws QueryException when a requested course is not found`() = runBlocking {
        val input = ScheduleByCourseInput(
            school = "miami", term = "202410", campus = listOf("O"),
            courses = listOf("CSE 271", "ENG 111")
        )

        val cseCourse = Course(subject = "CSE", courseNum = "271", crn = 1)
        
        // Mock to return CSE but not ENG
        whenever(connector.getCourseByInfo(any<com.tomdh.courseapi.course.CourseByInfoInput>())).thenAnswer { invocation ->
            val req = invocation.arguments[0] as com.tomdh.courseapi.course.CourseByInfoInput
            if (req.subject?.contains("CSE") == true) listOf(cseCourse) else emptyList<Course>()
        }

        assertThrows<QueryException> {
            combinator.getScheduleByCourses(input, connector)
        }
    }

    @Test
    fun `getFillerByAttributes finds valid fillers`() = runBlocking {
        val input = FillerByAttributesInput(
            school = "miami", term = "202410", campus = listOf("O"),
            courses = listOf("CSE 271"), attributes = listOf("PA1C")
        )

        val cseCourse = Course(
            subject = "CSE", courseNum = "271", crn = 1,
            delivery = "MWF 10:05 am-11:00 am"
        )
        val fillerCourse = Course(
            subject = "ENG", courseNum = "111", crn = 2,
            delivery = "TR 10:05 am-11:00 am"
        )

        whenever(connector.getCourseByInfo(any<com.tomdh.courseapi.course.CourseByInfoInput>())).thenReturn(listOf(cseCourse))
        whenever(cache.fetchAttributes(any())).thenReturn(listOf(fillerCourse))

        val result = combinator.getFillerByAttributes(input, connector)
        
        // Since filler is on TR and core course is on MWF, they do not conflict.
        assertEquals(1, result.size)
        assertTrue(result[0].courses.any { it.crn == 2 })
    }
}
