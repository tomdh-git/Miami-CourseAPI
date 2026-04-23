package com.tomdh.courseapi.course

import com.tomdh.courseapi.school.SchoolConnector
import com.tomdh.courseapi.school.SchoolRegistry
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest
class CourseCacheIntegrationTests {

    @MockBean
    lateinit var schoolRegistry: SchoolRegistry

    @MockBean
    lateinit var schoolConnector: SchoolConnector

    @Autowired
    lateinit var courseService: CourseService
    
    private fun testSection() = SchedulableSection(
        name = "CSE 271 - OOP",
        timeWindows = listOf(CanonicalTimeWindow("MONDAY", "10:00am", "10:50am")),
        data = mapOf("subject" to "CSE", "courseNum" to "271", "crn" to 12345)
    )

    @Test
    fun `getCourses caches results based on filters`() = runBlocking {
        val filters = mapOf<String, Any?>("term" to "202410", "campus" to listOf("O"))

        whenever(schoolRegistry.getConnector("miami")).thenReturn(schoolConnector)
        whenever(schoolConnector.validateFilters(any())).thenReturn(emptyList())
        whenever(schoolConnector.queryCourses(filters)).thenReturn(listOf(testSection()))

        // First call - should hit the mock
        courseService.getCourses("miami", filters, 100)
        
        // Second call - should hit the cache
        courseService.getCourses("miami", filters, 100)
        
        // Third call - should hit the cache
        courseService.getCourses("miami", filters, 100)

        // Verify upstream connector is only queried exactly ONCE
        verify(schoolConnector, times(1)).queryCourses(filters)
    }
}
