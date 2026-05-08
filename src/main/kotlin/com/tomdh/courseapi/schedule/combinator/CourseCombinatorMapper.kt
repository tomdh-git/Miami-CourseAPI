package com.tomdh.courseapi.schedule.combinator

import com.tomdh.schoolconnector.course.SchedulableSection
import com.tomdh.intervalcombinator.model.CombinatorItem
import com.tomdh.intervalcombinator.model.TimeWindow
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

    private fun parseTime(time: String): LocalTime {
        return LocalTime.parse(time.replace(" ", ""), timeFormatter)
    }

    fun mapToCombinatorGroup(sections: List<SchedulableSection>): List<CombinatorItem<SchedulableSection>> {
        return sections.map { section ->
            val windows = section.timeWindows.mapNotNull { tw ->
                try {
                    val day = DayOfWeek.valueOf(tw.day)
                    TimeWindow(
                        day,
                        parseTime(tw.startTime),
                        parseTime(tw.endTime)
                    )
                } catch (_: Exception) { null }
            }
            CombinatorItem(payload = section, windows = windows)
        }
    }
}
