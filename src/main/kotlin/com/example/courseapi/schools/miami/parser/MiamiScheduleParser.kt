package com.example.courseapi.schools.miami

fun parseCourses(courses: List<String>): List<Pair<String,String>>{
    return courses.mapNotNull {
        val p = it
            .trim()
            .split(" ")
        val courseValid = p.size == 2
        if (courseValid) p[0] to p[1]
        else null
    }
}