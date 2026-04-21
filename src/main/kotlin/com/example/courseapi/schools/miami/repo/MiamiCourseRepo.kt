package com.example.courseapi.schools.miami

import com.example.courseapi.exceptions.*
import com.example.courseapi.course.Course
import com.example.courseapi.course.CourseByInfoInput
import com.example.courseapi.schools.miami.formRequest
import com.example.courseapi.schools.miami.*
import org.springframework.stereotype.Repository

@Repository
class MiamiCourseRepo(private val requests: MiamiClient, private val parse: MiamiCourseParser){
    suspend fun getCourseByInfo(input: CourseByInfoInput): List<Course> {
        val token: String = requests.getOrFetchToken()
        if (token.isEmpty()) throw APIException("Empty Token")
        
        val formParts = ArrayList<String>(24)
        val formBody = formRequest(formParts, input, token).joinToString("&")
        var resp = requests.postResultResponse(formBody)
        val isExpired = resp.status == 419 || resp.body.contains("Page Expired", ignoreCase = true)
        if (isExpired) {
            val token = requests.getToken()
            if (token.isNotEmpty()) {
                formParts[0] = "_token=${java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8)}"
                val formBody = formParts.joinToString("&")
                resp = requests.postResultResponse(formBody)
            }
        }

        val hasTooManyResults = resp.body.contains("Your query returned too many results.", ignoreCase = true)
        if (hasTooManyResults) { throw QueryException("Query returned too many results.") }
        
        return parse.parseCourses(resp.body)
    }
}
