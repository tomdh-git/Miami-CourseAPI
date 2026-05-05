package com.tomdh.courseapi.schedule

import com.tomdh.courseapi.course.SchedulableSection
import com.tomdh.courseapi.exceptions.types.QueryException
import com.tomdh.courseapi.school.SchoolConnector
import com.tomdh.intervalcombinator.dsl.IntervalCombinator
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component
import java.time.LocalTime
import java.time.format.DateTimeFormatterBuilder

@Component
class ScheduleCombinator(private val cache: FillerAttributeCache) {

    private val timeFormatter = DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern("h:mma")
        .toFormatter()

    private fun parseCourses(courses: List<String>): List<Pair<String, String>> {
        return courses.mapNotNull {
            val p = it.trim().split(" ")
            if (p.size == 2) p[0] to p[1] else null
        }
    }

    private fun parseTime(timeString: String?): LocalTime? {
        if (timeString.isNullOrBlank()) return null
        return try {
            LocalTime.parse(
                timeString.replace(" ", ""),
                timeFormatter
            )
        } catch (_: Exception) { null }
    }

    /**
     * Generates all valid, non-conflicting schedule combinations from a given list of desired courses.
     * Combines intervals using the IntervalCombinator and filters by preferred time bounds.
     */
    suspend fun getScheduleByCourses(
        input: ScheduleQueryInput,
        connector: SchoolConnector
    ): List<Schedule> {
        val parsedCourses = parseCourses(input.courses)
        val fetched = fetchCourses(parsedCourses, input, connector)
        val valid = fetched.filterValues { it.isNotEmpty() }

        if (valid.size < parsedCourses.toSet().size) throw QueryException("Could not find valid sections for all requested courses")
        if (valid.isEmpty()) throw QueryException("No valid schedules found")

        val startBound = parseTime(input.preferredStart) ?: LocalTime.MIN
        val endBound = parseTime(input.preferredEnd) ?: LocalTime.MAX

        val combinatorResults = IntervalCombinator.generate<SchedulableSection> {
            groups = valid.values.map { CourseCombinatorMapper.mapToCombinatorGroup(it) }
            constraints {
                globalStart = startBound
                globalEnd = endBound
            }
            algorithms {
                optimizeFreeTime = input.optimizeFreeTime == true
                maxResults = 100
            }
        }

        if (combinatorResults.isEmpty()) throw QueryException("No valid schedule combos found")
        return combinatorResults.map { Schedule(sections = it.items, freeTime = it.freeTimeMinutes) }
    }

    /**
     * Finds "filler" courses that can fit into the "free time" gaps of an existing schedule.
     * Uses the fillerFilters from the input to query for candidate courses.
     */
    suspend fun getFillerSchedules(
        input: ScheduleQueryInput,
        connector: SchoolConnector
    ): List<Schedule> = coroutineScope {
        val fillerFilters = input.fillerFilters ?: throw QueryException("fillerFilters required for filler search")

        val fillersDeferred = async { cache.fetchFillerCourses(connector, fillerFilters) }
        val schedulesDeferred = async { getScheduleByCourses(input, connector) }

        val fillerSections = fillersDeferred.await()
        val schedules = schedulesDeferred.await()
        val startBound = parseTime(input.preferredStart) ?: LocalTime.MIN
        val endBound = parseTime(input.preferredEnd) ?: LocalTime.MAX
        val allFillers = CourseCombinatorMapper.mapToCombinatorGroup(fillerSections)

        schedules.map { schedule ->
            val existing = CourseCombinatorMapper.mapToCombinatorGroup(schedule.sections)
            val compatible = IntervalCombinator.findFillers<SchedulableSection> {
                this.existing = existing
                candidates = allFillers
                constraints { globalStart = startBound; globalEnd = endBound }
            }
            if (compatible.isNotEmpty()) Schedule(schedule.sections + compatible.first().payload, schedule.freeTime)
            else schedule
        }
    }

    private suspend fun fetchCourses(
        parsed: List<Pair<String, String>>,
        input: ScheduleQueryInput,
        connector: SchoolConnector
    ): Map<Pair<String, String>, List<SchedulableSection>> = coroutineScope {
        parsed.map { (subject, num) ->
            async {
                // Build a per-course filter map by merging the base filters with the specific subject/courseNum
                val courseFilters = input.filters.toMutableMap().apply {
                    put("subject", listOf(subject))
                    put("courseNum", num)
                }
                Pair(
                    Pair(subject, num),
                    connector.queryCourses(courseFilters)
                )
            }
        }.awaitAll()
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.flatten() }
    }
}
