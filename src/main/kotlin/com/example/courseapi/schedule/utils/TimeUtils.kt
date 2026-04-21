package com.example.courseapi.schedule

import com.example.courseapi.course.Course
import com.example.courseapi.schedule.Schedule
import kotlin.text.iterator

val timeSlotRegex = Regex(
    """([MTWRFSU]+)\s+(\d{1,2}:\d{2}[ap]m)-(\d{1,2}:\d{2}[ap]m)""",
    RegexOption.IGNORE_CASE
)
enum class Day { M, T, W, R, F, S, U }
data class Interval(
    val day: Day,
    val start: Int,
    val end: Int
)

fun toMinutes(t: String): Int {
    var i = 0
    val s = t.trim()
    var h = 0
    var m = 0
    while (i < s.length && s[i].isDigit()) {
        h = h * 10 + (s[i] - '0')
        i++
    }
    if (i >= s.length || s[i] != ':') return 0

    i++
    while (i < s.length && s[i].isDigit()) {
        m = m * 10 + (s[i] - '0')
        i++
    }

    var isPm = false
    if (i < s.length - 1) {
        val ch1 = s[i]
        val charAfter = i + 1 < s.length
        val ch2 = if (charAfter) s[i + 1] else ' '

        isPm = (ch1 == 'p' || ch1 == 'P')
                && (ch2 == 'm' || ch2 == 'M')
    }
    if (h == 12) h = 0
    if (isPm) h += 12
    return h * 60 + m
}

fun charToDay(c: Char) = when (c) {
    'M' -> Day.M; 'T' -> Day.T; 'W' -> Day.W; 'R' -> Day.R
    'F' -> Day.F; 'S' -> Day.S; 'U' -> Day.U
    else -> null
}

fun parseTimeSlot(slot: String): Sequence<Interval> = sequence {
    val matches = timeSlotRegex.findAll(slot).toList()
    for ((i, m) in matches.withIndex()) {
        val groupValues = m.groupValues
        val daysStr = groupValues[1]
        val start = toMinutes(groupValues[2])
        val end = toMinutes(groupValues[3])

        // Get the text between this match's end and the next match's start (or end of string)
        val segmentEnd = if (i + 1 < matches.size) matches[i + 1].range.first else slot.length
        val afterText = slot.substring(m.range.last + 1, segmentEnd)

        // Skip final exam entries: they have a single date (MM/DD) but no date range (MM/DD - MM/DD)
        val hasDateRange = afterText.contains(Regex("""\d{2}/\d{2}\s*-\s*\d{2}/\d{2}"""))
        val hasSingleDate = afterText.contains(Regex("""\d{2}/\d{2}"""))
        if (hasSingleDate && !hasDateRange) continue

        for (dayChar in daysStr) {
            val day = charToDay(dayChar) ?: continue
            yield(Interval(day, start, end))
        }
    }
}

fun freeTimeForSchedule(courses: List<Course>): Int {
    val map = arrayOfNulls<MutableList<Pair<Int, Int>>?>(7)
    for (c in courses) {
        val d = c.delivery
        for (iv in parseTimeSlot(d)) {
            val dayIndex = iv.day.ordinal
            val dayEmpty = map[dayIndex] == null
            if (dayEmpty) map[dayIndex] = mutableListOf()
            map[dayIndex]!!.add(iv.start to iv.end)
        }
    }

    var total = 0
    for (dayIndex in 0 until 7) {
        val intervals = map[dayIndex]
        val intervalsValid = intervals != null && intervals.isNotEmpty()
        if (intervalsValid) {
            intervals.sortBy { it.first }
            var last = 7 * 60
            for ((start, end) in intervals) {
                if (start > last) total += start - last
                last = maxOf(last, end)
            }
            total += (23 * 60) - last
        } else {
            total += (23 * 60) - (7 * 60) // Full day free if no classes
        }
    }
    return total
}

fun getFreeSlots(schedule: Schedule, startMin: Int, endMin: Int): Map<Day, List<Pair<Int, Int>>> {
    val dayBusy = mutableMapOf<Day, MutableList<Pair<Int, Int>>>()
    for (c in schedule.courses) {
        for (iv in parseTimeSlot(c.delivery)) {
            dayBusy.computeIfAbsent(iv.day) { mutableListOf() }.add(iv.start to iv.end)
        }
    }

    val free = mutableMapOf<Day, List<Pair<Int, Int>>>()
    for (day in Day.entries) {
        val busy = dayBusy[day]?.sortedBy { it.first } ?: emptyList()
        val dailyFree = mutableListOf<Pair<Int, Int>>()
        var cur = startMin
        for ((bs, be) in busy) {
            val currValid = endMin in (bs + 1)..<be
            if (currValid) {
                cur = endMin
                break
            }

            val inBounds = be <= startMin || bs >= endMin
            if (inBounds) continue

            val bsClipped = maxOf(bs, startMin)
            if (bsClipped > cur) {
                val freeEnd = minOf(bsClipped, endMin)
                if (freeEnd > cur) {
                    dailyFree.add(cur to freeEnd)
                }
            }
            cur = maxOf(
                cur,
                minOf(
                    be,
                    endMin
                ))
            if (cur >= endMin) break
        }
        if (cur < endMin) dailyFree.add(cur to endMin)
        free[day] = dailyFree
    }
    return free
}