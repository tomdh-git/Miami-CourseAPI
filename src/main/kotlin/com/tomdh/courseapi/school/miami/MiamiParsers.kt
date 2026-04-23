package com.tomdh.courseapi.school.miami

import com.tomdh.courseapi.course.CanonicalTimeWindow
import com.tomdh.courseapi.course.SchedulableSection
import com.tomdh.courseapi.field.Field
import org.jsoup.Jsoup.parse

/**
 * Miami-specific HTML parsing. Produces [SchedulableSection]s with canonical
 * fields and preserves all Miami-specific data in [SchedulableSection.data].
 */

private val timeSlotRegex = Regex(
    """([MTWRFSU]+)\s+(\d{1,2}:\d\d[ap]m)-(\d{1,2}:\d\d[ap]m)""",
    RegexOption.IGNORE_CASE
)

private val dayCharToName = mapOf(
    'M' to "MONDAY",
    'T' to "TUESDAY",
    'W' to "WEDNESDAY",
    'R' to "THURSDAY",
    'F' to "FRIDAY",
    'S' to "SATURDAY",
    'U' to "SUNDAY"
)

/**
 * Parses Miami's delivery string (e.g. "MWF 10:00am-10:50am  01/13-05/02")
 * into canonical [CanonicalTimeWindow]s.
 */
fun parseMiamiDeliveryToTimeWindows(delivery: String): List<CanonicalTimeWindow> {
    val windows = mutableListOf<CanonicalTimeWindow>()
    val matches = timeSlotRegex.findAll(delivery).toList()

    for ((i, m) in matches.withIndex()) {
        val (_, daysStr, startTime, endTime) = m.groupValues
        val segmentEnd = if (i + 1 < matches.size) matches[i + 1].range.first else delivery.length
        val afterText = delivery.substring(m.range.last + 1, segmentEnd)

        val hasDateRange = afterText.contains(Regex("""\d{2}/\d{2}\s*-\s*\d{2}/\d{2}"""))
        val hasSingleDate = afterText.contains(Regex("""\d{2}/\d{2}"""))
        if (hasSingleDate && !hasDateRange) continue

        for (dayChar in daysStr) {
            val dayName = dayCharToName[dayChar.uppercaseChar()] ?: continue
            windows.add(CanonicalTimeWindow(day = dayName, startTime = startTime, endTime = endTime))
        }
    }
    return windows
}

fun String.parseMiamiCoursesToSections(): List<SchedulableSection> {
    val rows = parse(this).select("tr.resultrow")
    return rows.mapNotNull { row ->
        val cells = row.select("td")
        if (cells.size < 9) return@mapNotNull null

        val subject = cells[0].ownText().trim()
        val courseNum = cells[1].text().trim()
        val title = cells[2].text().trim()
        if (subject.isEmpty() && courseNum.isEmpty() && title.isEmpty()) return@mapNotNull null

        val section = cells[3].text().trim()
        val crn = cells[4].text().trim().filter { it.isDigit() }.toIntOrNull() ?: 0
        val campus = cells[5].text().trim()
        val credits = cells[6].text().trim().toIntOrNull() ?: 0
        val capacity = cells[7].text().trim()
        val requests = cells[8].text().trim()
        val delivery = cells.getOrNull(9)?.text()?.trim() ?: ""

        val data = mapOf<String, Any?>(
            "subject" to subject,
            "courseNum" to courseNum,
            "title" to title,
            "section" to section,
            "crn" to crn,
            "campus" to campus,
            "credits" to credits,
            "capacity" to capacity,
            "requests" to requests,
            "delivery" to delivery,
            "details" to ""
        )

        SchedulableSection(
            name = "$subject $courseNum - $title",
            timeWindows = parseMiamiDeliveryToTimeWindows(delivery),
            data = data
        )
    }
}

fun String.parseMiamiTerms(): List<Field> {
    return parse(this).select("select#termFilter option[value]").map { Field(it.attr("value").trim()) }
}

/**
 * ValidFields for Miami — kept internal to the Miami connector.
 */
data class MiamiValidFields(
    val subjects: Set<String>,
    val campuses: Set<String>,
    val terms: Set<String>,
    val deliveryTypes: Set<String>,
    val levels: Set<String>,
    val days: Set<String>,
    val waitlistTypes: Set<String>,
    val attributes: Set<String>
)

fun String.parseMiamiFields(): MiamiValidFields {
    val doc = parse(this)
    return MiamiValidFields(
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
