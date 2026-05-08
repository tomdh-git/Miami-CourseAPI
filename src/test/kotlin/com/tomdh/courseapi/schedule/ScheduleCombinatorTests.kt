package com.tomdh.courseapi.schedule

import com.tomdh.courseapi.config.CourseApiProperties
import com.tomdh.courseapi.generated.types.ScheduleQueryInput
import com.tomdh.courseapi.schedule.combinator.FillerAttributeCache
import com.tomdh.courseapi.schedule.combinator.ScheduleCombinator
import com.tomdh.schoolconnector.course.SchedulableSection
import com.tomdh.schoolconnector.exceptions.types.QueryException
import com.tomdh.schoolconnector.school.SchoolConnector
import com.tomdh.schoolconnector.school.miami.parseMiamiDeliveryToTimeWindows
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@Suppress("UNCHECKED_CAST")
@ExtendWith(MockitoExtension::class)
class ScheduleCombinatorTests {

    @Mock lateinit var cache: FillerAttributeCache
    @Mock lateinit var connector: SchoolConnector
    private val properties = CourseApiProperties()

    private val combinator by lazy { ScheduleCombinator(cache, properties) }

    private fun filters(vararg pairs: Pair<String, Any?>): Object =
        mapOf(*pairs) as Object

    private fun section(subject: String, num: String, delivery: String): SchedulableSection {
        val timeWindows = parseMiamiDeliveryToTimeWindows(delivery)
        return SchedulableSection(
            name = "$subject $num",
            timeWindows = timeWindows,
            data = mapOf("subject" to subject, "courseNum" to num, "delivery" to delivery)
        )
    }

    @Test
    fun `getScheduleByCourses correctly combines courses and filters by time`() = runBlocking {
        val input = ScheduleQueryInput(
            school = "miami",
            filters = filters("term" to "202410", "campus" to listOf("O")),
            courses = listOf("CSE 271", "MTH 251"),
            preferredStart = "9:00am",
            preferredEnd = "5:00pm",
            optimizeFreeTime = true
        )

        val cseSection = section("CSE", "271", "MWF 10:05am-11:00am")
        val mthSection = section("MTH", "251", "MWF 11:40am-12:35pm")

        whenever(connector.queryCoursesLight(any())).thenAnswer { invocation ->
            val filters = invocation.arguments[0] as Map<*, *>
            val subjects = filters["subject"] as? List<*>
            when {
                subjects?.contains("CSE") == true -> listOf(cseSection)
                subjects?.contains("MTH") == true -> listOf(mthSection)
                else -> emptyList<SchedulableSection>()
            }
        }

        val result = combinator.getScheduleByCourses(input, connector)

        assertEquals(1, result.size)
        assertEquals(2, result[0].sections.size)
    }

    @Test
    fun `getScheduleByCourses throws QueryException when a requested course is not found`() = runBlocking {
        val input = ScheduleQueryInput(
            school = "miami",
            filters = filters("term" to "202410", "campus" to listOf("O")),
            courses = listOf("CSE 271", "ENG 111")
        )

        val cseSection = section("CSE", "271", "MWF 10:05am-11:00am")

        whenever(connector.queryCoursesLight(any())).thenAnswer { invocation ->
            val filters = invocation.arguments[0] as Map<*, *>
            val subjects = filters["subject"] as? List<*>
            if (subjects?.contains("CSE") == true) listOf(cseSection) else emptyList<SchedulableSection>()
        }

        assertThrows<QueryException> {
            combinator.getScheduleByCourses(input, connector)
        }
    }

    @Test
    fun `getFillerSchedules finds valid fillers`() = runBlocking {
        val input = ScheduleQueryInput(
            school = "miami",
            filters = filters("term" to "202410", "campus" to listOf("O")),
            courses = listOf("CSE 271"),
            fillerFilters = mapOf("attributes" to listOf("PA1C")) as Object
        )

        val cseSection = section("CSE", "271", "MWF 10:05am-11:00am")
        val fillerSection = section("ENG", "111", "TR 10:05am-11:00am")

        whenever(connector.queryCoursesLight(any())).thenReturn(listOf(cseSection))
        whenever(cache.fetchFillerCourses(any(), any())).thenReturn(listOf(fillerSection))

        val result = combinator.getFillerSchedules(input, connector)

        assertEquals(1, result.size)
        assertTrue(result[0].sections.any { it.name.contains("ENG") })
    }
}
