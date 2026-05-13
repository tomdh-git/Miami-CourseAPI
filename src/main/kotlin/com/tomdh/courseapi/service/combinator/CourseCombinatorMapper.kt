package com.tomdh.courseapi.service.combinator

import com.tomdh.schoolconnector.course.SchedulableSection
import com.tomdh.intervalcombinator.model.CombinatorItem
import com.tomdh.intervalcombinator.model.TimeWindow
import com.tomdh.schoolconnector.course.CanonicalTimeWindow
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatterBuilder

/**
 * Maps [SchedulableSection]s (with pre-parsed canonical time windows)
 * into [CombinatorItem]s for the IntervalCombinator.
 *
 * No longer Miami-specific — works with any school's data since
 * time windows are already in canonical format.
 */
object CourseCombinatorMapper {

    private val timeFormatter = DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern("h:mma")
        .toFormatter()

    private fun parseTime(time: String): LocalTime =
        LocalTime.parse(time.replace(" ", ""), timeFormatter)

    private fun parseTimeWindow(tw: CanonicalTimeWindow): TimeWindow? =
        try {
            TimeWindow(
                day = DayOfWeek.valueOf(tw.day),
                start = parseTime(tw.startTime),
                end = parseTime(tw.endTime)
            )
        } catch (_: Exception) {
            null
        }

    fun mapToCombinatorGroup(sections: List<SchedulableSection>): List<CombinatorItem<SchedulableSection>> =
        sections.map { section ->
            CombinatorItem(
                payload = section,
                windows = section.timeWindows.mapNotNull(::parseTimeWindow)
            )
        }
}
