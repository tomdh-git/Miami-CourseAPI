package com.tomdh.courseapi.schedule

import com.tomdh.courseapi.course.Course
import com.tomdh.intervalcombinator.model.CombinatorItem
import com.tomdh.intervalcombinator.model.TimeWindow

object CourseCombinatorMapper {

    private val timeSlotRegex = Regex(
        """([MTWRFSU]+)\s+(\d{1,2}:\d\d[ap]m)-(\d{1,2}:\d\d[ap]m)""",
        RegexOption.IGNORE_CASE
    )

    fun mapToCombinatorGroup(courses: List<Course>): List<CombinatorItem<Course>> {
        return courses.map { course ->
            val windows = mutableListOf<TimeWindow>()
            val slot = course.delivery
            val matches = timeSlotRegex
                .findAll(slot)
                .toList()

            for ((i, m) in matches.withIndex()) {
                val (_, daysStr, startTime, endTime) = m.groupValues
                val segmentEnd = if (i + 1 < matches.size) matches[i + 1].range.first else slot.length
                val afterText = slot.substring(m.range.last + 1, segmentEnd)

                val hasDateRange = afterText.contains(Regex("""\d{2}/\d{2}\s*-\s*\d{2}/\d{2}"""))
                val hasSingleDate = afterText.contains(Regex("""\d{2}/\d{2}"""))
                if (hasSingleDate && !hasDateRange) continue

                for (dayChar in daysStr) {
                    try { windows.add(TimeWindow.parse(dayChar, startTime, endTime)) } catch (_: Exception) { }
                }
            }

            CombinatorItem(payload = course, windows = windows)
        }
    }
}
