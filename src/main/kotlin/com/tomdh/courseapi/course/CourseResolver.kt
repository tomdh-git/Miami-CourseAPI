package com.tomdh.courseapi.course

import com.tomdh.courseapi.exceptions.*
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import org.slf4j.LoggerFactory

@DgsComponent
class CourseResolver(private val service: CourseService) {
    private val logger = LoggerFactory.getLogger(CourseResolver::class.java)

    @DgsQuery
    suspend fun getCourseByInfo(@InputArgument input: CourseByInfoInput, @InputArgument limit: Int?): CourseResult {
        return resolveQuery(
            logger,
            ::ErrorCourse
        ) {
            SuccessCourse(
                service.getCourseByInfo(input)
                    .take(limit ?: 100)
            )
        }
    }

    @DgsQuery
    suspend fun getCourseByCRN(@InputArgument input: CourseByCRNInput): CourseResult {
        return resolveQuery(
            logger,
            ::ErrorCourse
        ) {
            SuccessCourse(
                service.getCourseByCRN(input)
            )
        }
    }
}
