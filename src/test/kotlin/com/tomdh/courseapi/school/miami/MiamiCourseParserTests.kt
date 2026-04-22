package com.tomdh.courseapi.school.miami

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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

    @Test
    fun `parseMiamiFields retrieves option data from HTML select`() {
        val html = """
            <select id="termFilter">
                <option value="202410">Fall Semester 2023-24</option>
                <option value="202420">Spring Semester 2023-24</option>
            </select>
        """.trimIndent()

        val fields = html.parseMiamiFields()
        
        assertEquals(2, fields.terms.size)
        assertTrue(fields.terms.contains("202410"))
        assertTrue(fields.terms.contains("202420"))
    }
    @Test
    fun `parseMiamiTerms extracts term fields`() {
        val html = """
            <select id="termFilter">
                <option value="202410">Fall Semester 2023-24</option>
            </select>
        """.trimIndent()
        val terms = html.parseMiamiTerms()
        assertEquals(1, terms.size)
        assertEquals("202410", terms[0].name)
    }
}
