package com.tomdh.courseapi.course

import com.tomdh.courseapi.exceptions.types.QueryException
import com.tomdh.courseapi.exceptions.types.ValidationException
import com.tomdh.courseapi.school.SchoolRegistry
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class CourseService(private val registry: SchoolRegistry) {

    @Cacheable(value = ["courses"], key = "{#school, #filters, #limit}")
    suspend fun getCourses(school: String, filters: Map<String, Any?>, limit: Int): List<SchedulableSection> {
        val connector = registry.getConnector(school)

        val errors = connector.validateFilters(filters)
        if (errors.isNotEmpty()) throw ValidationException(errors)

        return connector.queryCourses(filters)
            .take(limit)
            .ifEmpty { throw QueryException("No courses found matching filters") }
    }
}
