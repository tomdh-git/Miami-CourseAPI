package com.tomdh.courseapi.school.miami

import com.tomdh.courseapi.course.Course
import com.tomdh.courseapi.field.Field
import com.tomdh.courseapi.field.ValidFields
import org.jsoup.Jsoup

fun String.parseMiamiCourses(): List<Course> {
    val rows = Jsoup.parse(this).select("tr.resultrow")
    return rows.mapNotNull { row ->
        val cells = row.select("td")
        if (cells.size < 9) return@mapNotNull null

        val subject = cells[0].ownText().trim()
        val courseNum = cells[1].text().trim()
        val title = cells[2].text().trim()
        if (subject.isEmpty() && courseNum.isEmpty() && title.isEmpty()) return@mapNotNull null

        Course(
            subject = subject, courseNum = courseNum, title = title,
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

fun String.parseMiamiTerms(): List<Field> {
    return Jsoup.parse(this).select("select#termFilter option[value]").map { Field(it.attr("value").trim()) }
}

fun String.parseMiamiFields(): ValidFields {
    val doc = Jsoup.parse(this)
    return ValidFields(
        subjects = doc.select("select#subject option[value]").map { it.attr("value").trim() }.filter { it.isNotEmpty() }.toSet(),
        campuses = doc.select("select#campusFilter option[value]").map { it.attr("value").trim() }.filter { it.isNotEmpty() }.toSet(),
        terms = doc.select("select#termFilter option[value]").map { it.attr("value").trim() }.toSet(),
        deliveryTypes = doc.select("input.deliveryTypeCheckBox[value]").map { it.attr("value").trim() }.filter { it.isNotEmpty() }.toSet(),
        levels = doc.select("select#levelFilter option[value]").map { it.attr("value").trim() }.toSet(),
        days = doc.select("select#daysFilter option[value]").map { it.attr("value").trim() }.toSet(),
        waitlistTypes = doc.select("select#openWaitlist option[value]").map { it.attr("value").trim() }.toSet(),
        attributes = doc.select("select#sectionFilterAttributes option[value]").map { it.attr("value").trim() }.toSet()
    )
}
