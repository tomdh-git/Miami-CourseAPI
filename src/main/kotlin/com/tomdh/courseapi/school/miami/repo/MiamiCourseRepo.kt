package com.tomdh.courseapi.school.miami.repo

import com.tomdh.courseapi.course.model.Course
import com.tomdh.courseapi.course.model.input.CourseByInfoInput
import com.tomdh.courseapi.exceptions.APIException
import com.tomdh.courseapi.exceptions.QueryException
import com.tomdh.courseapi.school.miami.client.MiamiClient
import com.tomdh.courseapi.school.miami.client.formRequest
import com.tomdh.courseapi.school.miami.parser.MiamiCourseParser
import org.springframework.stereotype.Repository

@Repository
class MiamiCourseRepo(
    private val requests: MiamiClient,
    private val parse: MiamiCourseParser
) {
    suspend fun getCourseByInfo(input: CourseByInfoInput): List<Course> {
        val token: String = requests.getOrFetchToken()
        if (token.isEmpty()) throw APIException("Empty Token")

        val formParts = ArrayList<String>(24)
        val formBody = formRequest(formParts, input, token).joinToString("&")
        var resp = requests.postResultResponse(formBody)

        val isExpired = resp.status == 419 || resp.body.contains("Page Expired", ignoreCase = true)
        if (isExpired) {
            val freshToken = requests.forceFetchToken()
            if (freshToken.isNotEmpty()) {
                formParts[0] = "_token=${java.net.URLEncoder.encode(freshToken, java.nio.charset.StandardCharsets.UTF_8)}"
                val retryBody = formParts.joinToString("&")
                resp = requests.postResultResponse(retryBody)
            }
        }

        val hasTooManyResults = resp.body.contains("Your query returned too many results.", ignoreCase = true)
        if (hasTooManyResults) {
            throw QueryException("Query returned too many results.")
        }

        return parse.parseCourses(resp.body)
    }
}
