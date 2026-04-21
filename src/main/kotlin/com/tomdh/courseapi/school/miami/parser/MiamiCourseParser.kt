package com.tomdh.courseapi.school.miami.parser

import com.tomdh.courseapi.school.CourseParser
import com.tomdh.courseapi.course.model.Course
import org.jsoup.Jsoup
import org.springframework.stereotype.Component

@Component
class MiamiCourseParser : CourseParser {

    override fun parseCourses(raw: String): List<Course> {
        val doc = Jsoup.parse(raw)
        val rows = doc.select("tr.resultrow")
        return rows.mapNotNull { row ->
            val cells = row.select("td")
            if (cells.size < 9) return@mapNotNull null
            
            val subject = cells[0].ownText().trim()
            val courseNum = cells[1].text().trim()
            val title = cells[2].text().trim()
            
            if (subject.isEmpty() && courseNum.isEmpty() && title.isEmpty()) return@mapNotNull null
            
            Course(
                subject = subject,
                courseNum = courseNum,
                title = title,
                section = cells[3].text().trim(),
                crn = cells[4].text().trim().filter { it.isDigit() }.toIntOrNull() ?: 0,
                campus = cells[5].text().trim(),
                credits = cells[6].text().trim().toIntOrNull() ?: 0,
                capacity = cells[7].text().trim(),
                requests = cells[8].text().trim(),
                delivery = cells.getOrNull(9)?.text()?.trim() ?: ""
            )
        }
    }
}