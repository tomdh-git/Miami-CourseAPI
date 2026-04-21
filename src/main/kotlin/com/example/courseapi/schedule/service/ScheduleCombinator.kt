package com.example.courseapi.schedule

import com.example.courseapi.config.SchoolConnector
import com.example.courseapi.course.Course
import com.example.courseapi.course.CourseByInfoInput
import com.example.courseapi.exceptions.QueryException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component

@Component
class ScheduleCombinator(private val cache: FillerAttributeCache) {

    private fun parseCourses(courses: List<String>): List<Pair<String,String>>{
        return courses.mapNotNull {
            val p = it.trim().split(" ")
            if (p.size == 2) p[0] to p[1] else null
        }
    }

    suspend fun getScheduleByCourses(input: ScheduleByCourseInput, connector: SchoolConnector): List<Schedule> {
        val parsedCourses = parseCourses(input.courses)
        val fetched = fetchCourses(parsedCourses, input, connector)
        val valid = fetched.filterValues { it.isNotEmpty() }
        
        val uniqueParsedCount = parsedCourses.toSet().size
        if (valid.size < uniqueParsedCount) {
             throw QueryException("Could not find valid sections for all requested courses")
        }
        
        if (valid.isEmpty()) throw QueryException("No valid schedules found")
        
        val startMin = toMinutes(input.preferredStart ?: "12:00am")
        val endMin = toMinutes(input.preferredEnd ?: "11:59pm")
        
        val schedules = generateValidSchedules(
            valid.values.toList(), 
            startMin, 
            endMin,
            optimizeFreeTime = input.optimizeFreeTime == true,
            maxResults = 100
        )
        if (schedules.isEmpty()) throw QueryException("No valid schedule combos found")
        return schedules
    }

    suspend fun getFillerByAttributes(input: FillerByAttributesInput, connector: SchoolConnector): List<Schedule> = coroutineScope {
        val attributesDeferred = async { cache.fetchAttributes(input) }
        val schedulesDeferred = async { getScheduleByCourses(input.toScheduleInput(), connector) }

        val attributesList = attributesDeferred.await()
        val schedules = schedulesDeferred.await()

        val startMin = toMinutes(input.preferredStart ?: "12:00am")
        val endMin = toMinutes(input.preferredEnd ?: "11:59pm")
        
        schedules.map { schedule ->
            addFillerCourse(schedule, attributesList, startMin, endMin)
        }
    }
    
    private suspend fun fetchCourses(parsed: List<Pair<String,String>>, input: ScheduleByCourseInput, connector: SchoolConnector): Map<Pair<String, String>, List<Course>> = coroutineScope{
        return@coroutineScope parsed.map { (subject, num) ->
            async {
                val sections = connector.getCourseByInfo(
                    CourseByInfoInput(
                        school = input.school,
                        delivery = input.delivery,
                        subject = listOf(subject),
                        courseNum = num,
                        campus = input.campus,
                        term = input.term
                    )
                )
                subject to num to sections
            }
        }.awaitAll().groupBy(
            { it.first },
            { it.second }
        ).mapValues { it.value.flatten() }
    }
}
