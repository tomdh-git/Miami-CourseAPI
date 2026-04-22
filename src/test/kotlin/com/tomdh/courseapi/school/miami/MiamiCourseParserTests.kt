package com.tomdh.courseapi.school.miami

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MiamiCourseParserTests {

    @Test
    fun `parseMiamiCourses retrieves course data from HTML table`() {
        val html = """
            <table id="courseSectionSummary" class="table">
                <tbody>
            <tr class="resultrow">
                <td>CSE</td>
                <td>174</td>
                <td>Intro to Programming</td>
                <td>A</td>
                <td>12345</td>
                <td>Oxford</td>
                <td>4</td>
                <td>25</td>
                <td>10</td>
                <td>MWF 10:05 am-11:00 am</td>
            </tr>
                </tbody>
            </table>
        """.trimIndent()

        val result = html.parseMiamiCourses()

        assertEquals(1, result.size)
        val course = result[0]
        assertEquals("CSE", course.subject)
        assertEquals("174", course.courseNum)
        assertEquals("A", course.section)
        assertEquals(12345, course.crn)
        assertEquals("Intro to Programming", course.title)
        assertEquals("MWF 10:05 am-11:00 am", course.delivery)
    }
}
