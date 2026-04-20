package com.example.courseapi.resolvers.course

import com.example.courseapi.models.course.CourseResult
import com.example.courseapi.models.dto.course.*
import com.example.courseapi.resolvers.utils.course.courseSafe
import com.example.courseapi.services.course.CourseService
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
    suspend fun getCourseByInfo(@Argument input: CourseByInfoInput): CourseResult =
        courseSafe { service.getCourseByInfo(input) }

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
    suspend fun getCourseByCRN(@Argument input: CourseByCRNInput): CourseResult =
        courseSafe { service.getCourseByCRN(input) }
}

