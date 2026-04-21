package com.example.courseapi.schedule

import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.CrossOrigin

 

@Controller
@CrossOrigin(origins = ["*"])
class ScheduleResolver(private val ss: ScheduleService) {

    /**
     * async function getScheduleByCourses
     * @param input: ScheduleByCourseInput (DTO)
     * @return ScheduleResult
     *      SuccessSchedule: List<Schedule>
     *      @throws: ErrorSchedule
     *
     * @description
     * calls safe wrapped getScheduleByCourses in ScheduleService
     *
     * @example
     * query{getScheduleByCourses(input:{}){... on SuccessSchedule{schedules:{}}... on ErrorSchedule{error,message}}}
     * */
    @QueryMapping
    suspend fun getScheduleByCourses(@Argument input: ScheduleByCourseInput): ScheduleResult {
        return runCatching { ss.getScheduleByCourses(input) }
            .fold(
                onSuccess = { SuccessSchedule(it) },
                onFailure = { ErrorSchedule(it.javaClass.simpleName, it.message ?: "Unknown error") }
            )
    }

    /**
     * async function getFillerByAttributes
     * @param input: FillerByAttributesInput (DTO)
     * @return ScheduleResult
     *      SuccessSchedule: List<Schedule>
     *      @throws: ErrorSchedule
     *
     * @description
     * calls safe wrapped getFillerByAttributes in ScheduleService
     *
     * @example
     * query{getFillerByAttributes(input:{}){... on SuccessSchedule{schedules:{}}... on ErrorSchedule{error,message}}}
     * */
    @QueryMapping
    suspend fun getFillerByAttributes(@Argument input: FillerByAttributesInput): ScheduleResult {
        return runCatching { ss.getFillerByAttributes(input) }
            .fold(
                onSuccess = { SuccessSchedule(it) },
                onFailure = { ErrorSchedule(it.javaClass.simpleName, it.message ?: "Unknown error") }
            )
    }

}