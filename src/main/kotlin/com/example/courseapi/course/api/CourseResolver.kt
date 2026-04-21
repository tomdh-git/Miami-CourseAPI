package com.example.courseapi.course

import org.springframework.graphql.data.method.annotation.*
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.CrossOrigin

/**
 * todo:
 * - you need to find out a way to get the description of a course and display it well on the frontend
 * - you need to also add getCourseByAttribute
 * - honors toggle
 * -
 */

 

@Controller
@CrossOrigin(origins = ["*"])
class CourseResolver(private val service: CourseService) {

    /**
     * async function getCourseByInfo
     * @param input: CourseByInfoInput (DTO)
     * @return CourseResult
     *      CourseSuccess: List<Course>
     *      @throws: CourseError
     *
     * @description
     * calls safe wrapped getCourseByInfo in CourseService
     *
     * @example
     * query{getCourseByInfo(input:{}){... on SuccessCourse{courses{}}... on ErrorCourse{error,message}}}
     * */
    @QueryMapping
    suspend fun getCourseByInfo(@Argument input: CourseByInfoInput): CourseResult {
        return runCatching { service.getCourseByInfo(input) }
            .fold(
                onSuccess = { SuccessCourse(it) },
                onFailure = { ErrorCourse(it.javaClass.simpleName, it.message ?: "Unknown error") }
            )
    }

    /**
     * async function getCourseByInfo
     * @param input: CourseByCRNInput (DTO)
     * @return: CourseResult
     *      CourseSuccess: List<Course>
     *      @throws: CourseError
     *
     * @description
     * calls safe wrapped getCourseByCRN in CourseService
     *
     * @example
     * query{getCourseByCRN(input:{}){... on SuccessCourse{courses{}}... on ErrorCourse{error,message}}}
     * */
    @QueryMapping
    suspend fun getCourseByCRN(@Argument input: CourseByCRNInput): CourseResult {
        return runCatching { service.getCourseByCRN(input) }
            .fold(
                onSuccess = { SuccessCourse(it) },
                onFailure = { ErrorCourse(it.javaClass.simpleName, it.message ?: "Unknown error") }
            )
    }
}

