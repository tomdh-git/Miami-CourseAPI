package com.tomdh.courseapi.school.miami

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MiamiCourseParserTests {

    @Test
    fun `parseMiamiCoursesToSections retrieves section data from HTML table`() {
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
                <td>MWF 10:05am-11:00am 01/13-05/02</td>
            </tr>
                </tbody>
            </table>
        """.trimIndent()

        val result = html.parseMiamiCoursesToSections()

        assertEquals(1, result.size)
        val section = result[0]
        assertEquals("CSE 174 - Intro to Programming", section.name)
        assertEquals("CSE", section.data["subject"])
        assertEquals("174", section.data["courseNum"])
        assertEquals(12345, section.data["crn"])
        assertTrue(section.timeWindows.isNotEmpty())
        assertEquals("MONDAY", section.timeWindows[0].day)
    }

    @Test
    fun `parseMiamiDeliveryToTimeWindows parses delivery string`() {
        val delivery = "MWF 10:05am-11:00am 01/13-05/02"
        val windows = parseMiamiDeliveryToTimeWindows(delivery)

        assertEquals(3, windows.size)
        assertEquals("MONDAY", windows[0].day)
        assertEquals("WEDNESDAY", windows[1].day)
        assertEquals("FRIDAY", windows[2].day)
        assertEquals("10:05am", windows[0].startTime)
        assertEquals("11:00am", windows[0].endTime)
    }

    @Test
    fun `parseMiamiDeliveryToTimeWindows skips single-date entries`() {
        val delivery = "MWF 10:05am-11:00am 01/13-05/02 R 2:00pm-3:00pm 03/15"
        val windows = parseMiamiDeliveryToTimeWindows(delivery)

        // Should only have MWF windows, not the single-date Thursday
        assertEquals(3, windows.size)
        assertTrue(windows.none { it.day == "THURSDAY" })
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
