package com.tomdh.courseapi.service.combinator

import com.tomdh.courseapi.config.CourseApiProperties
import com.tomdh.courseapi.generated.types.Schedule
import com.tomdh.courseapi.generated.types.ScheduleQueryInput
import com.tomdh.schoolconnector.course.SchedulableSection
import com.tomdh.schoolconnector.exceptions.types.QueryException
import com.tomdh.schoolconnector.school.SchoolConnector
import com.tomdh.intervalcombinator.dsl.IntervalCombinator
import com.tomdh.courseapi.scalar.JsonScalarUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component
import java.time.LocalTime
import java.time.format.DateTimeFormatterBuilder

@Component
class ScheduleCombinator(
  private val cache: FillerAttributeCache,
  private val properties: CourseApiProperties
) {

  private val timeFormatter = DateTimeFormatterBuilder()
    .parseCaseInsensitive()
    .appendPattern("h:mma")
    .toFormatter()

  private fun parseCourses(courses: List<String>): List<Pair<String, String>> {
    return courses.mapNotNull {
      val parts = it.trim().split(" ")
      if (parts.size == 2) parts[0] to parts[1] else null
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
    val filters = JsonScalarUtils.safeCastToMap(input.filters)

    val parsedCourses = parseCourses(input.courses)
    val fetchedSections = fetchCourses(parsedCourses, filters, connector)
    val validSections = fetchedSections.filterValues { it.isNotEmpty() }

    if (validSections.size < parsedCourses.toSet().size) throw QueryException("Could not find valid sections for all requested courses")
    if (validSections.isEmpty()) throw QueryException("No valid schedules found")

    val startBound = parseTime(input.preferredStart) ?: LocalTime.MIN
    val endBound = parseTime(input.preferredEnd) ?: LocalTime.MAX

    val combinatorResults = IntervalCombinator.generate<SchedulableSection> {
      groups = validSections.values.map { CourseCombinatorMapper.mapToCombinatorGroup(it) }
      constraints {
        globalStart = startBound
        globalEnd = endBound
      }
      algorithms {
        optimizeFreeTime = input.optimizeFreeTime == true
        maxResults = properties.schedule.maxCombinatorResults
      }
    }

    if (combinatorResults.isEmpty()) throw QueryException("No valid schedule combos found")

    val schedules = combinatorResults.map { Schedule(sections = it.items, freeTime = it.freeTimeMinutes) }

    // Fetch details only for sections that appear in valid schedules
    val term = filters["term"].toString()
    val uniqueSections = schedules.flatMap { it.sections }.distinctBy { it.data["crn"] }
    connector.fetchSectionDetails(term, uniqueSections)

    return schedules
  }

  /**
  * Finds "filler" courses that can fit into the "free time" gaps of an existing schedule.
  * Uses the fillerFilters from the input to query for candidate courses.
  */
  suspend fun getFillerSchedules(
    input: ScheduleQueryInput,
    connector: SchoolConnector
  ): List<Schedule> = coroutineScope {
    val fillerFilters = input.fillerFilters?.let { JsonScalarUtils.safeCastToMap(it) }
      ?.takeIf { it.isNotEmpty() }
      ?: throw QueryException("fillerFilters required for filler search")

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
    baseFilters: Map<String, Any?>,
    connector: SchoolConnector
  ): Map<Pair<String, String>, List<SchedulableSection>> = coroutineScope {
    val results: List<Pair<Pair<String, String>, List<SchedulableSection>>> = parsed.map { (subject, num) ->
      async {
        // Build a per-course filter map by merging the base filters with the specific subject/courseNum
        val courseFilters = baseFilters.toMutableMap().apply {
          put("subject", listOf(subject))
          put("courseNum", num)
        }
        Pair(
          Pair(subject, num),
          connector.queryCoursesLight(courseFilters)
        )
      }
    }.awaitAll()
    results
      .groupBy({ it.first }, { it.second })
      .mapValues { it.value.flatten() }
  }
}
