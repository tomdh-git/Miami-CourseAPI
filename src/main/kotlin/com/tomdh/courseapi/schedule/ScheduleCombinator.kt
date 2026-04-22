package com.tomdh.courseapi.schedule

import com.tomdh.courseapi.course.Course
import com.tomdh.courseapi.course.CourseByInfoInput
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
        return try { LocalTime.parse(timeString.replace(" ", ""), timeFormatter) } catch (_: Exception) { null }
    }

    suspend fun getScheduleByCourses(input: ScheduleByCourseInput, connector: SchoolConnector): List<Schedule> {
        val parsedCourses = parseCourses(input.courses)
        val fetched = fetchCourses(parsedCourses, input, connector)
        val valid = fetched.filterValues { it.isNotEmpty() }

        if (valid.size < parsedCourses.toSet().size) throw QueryException("Could not find valid sections for all requested courses")
        if (valid.isEmpty()) throw QueryException("No valid schedules found")

        val startBound = parseTime(input.preferredStart) ?: LocalTime.MIN
        val endBound = parseTime(input.preferredEnd) ?: LocalTime.MAX

        val combinatorResults = IntervalCombinator.generate<Course> {
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
        return combinatorResults.map { Schedule(courses = it.items, freeTime = it.freeTimeMinutes) }
    }

    suspend fun getFillerByAttributes(input: FillerByAttributesInput, connector: SchoolConnector): List<Schedule> = coroutineScope {
        val attributesDeferred = async { cache.fetchAttributes(input) }
        val schedulesDeferred = async { getScheduleByCourses(input.toScheduleInput(), connector) }

        val attributesList = attributesDeferred.await()
        val schedules = schedulesDeferred.await()
        val startBound = parseTime(input.preferredStart) ?: LocalTime.MIN
        val endBound = parseTime(input.preferredEnd) ?: LocalTime.MAX
        val allFillers = CourseCombinatorMapper.mapToCombinatorGroup(attributesList)

        schedules.map { schedule ->
            val existing = CourseCombinatorMapper.mapToCombinatorGroup(schedule.courses)
            val compatible = IntervalCombinator.findFillers<Course> {
                this.existing = existing
                candidates = allFillers
                constraints { globalStart = startBound; globalEnd = endBound }
            }
            if (compatible.isNotEmpty()) Schedule(schedule.courses + compatible.first().payload, schedule.freeTime)
            else schedule
        }
    }

    private suspend fun fetchCourses(parsed: List<Pair<String, String>>, input: ScheduleByCourseInput, connector: SchoolConnector): Map<Pair<String, String>, List<Course>> = coroutineScope {
        parsed.map { (subject, num) ->
            async {
                val query = CourseByInfoInput(school = input.school, delivery = input.delivery, subject = listOf(subject), courseNum = num, campus = input.campus, term = input.term)
                Pair(Pair(subject, num), connector.getCourseByInfo(query))
            }
        }.awaitAll().groupBy({ it.first }, { it.second }).mapValues { it.value.flatten() }
    }
}
