package com.tomdh.courseapi.school.miami


private fun String.encode(): String = java.net.URLEncoder.encode(this, java.nio.charset.StandardCharsets.UTF_8)

/**
 * Builds a URL-encoded form body from the generic filter map for Miami's course search.
 * Extracts known Miami fields from the map and encodes them as form parameters.
 */
fun buildFormRequest(formParts: ArrayList<String>, filters: Map<String, Any?>, token: String): ArrayList<String> {
    formParts.add("_token=${token.encode()}")
    formParts.add("term=${(filters["term"] as? String ?: "").encode()}")

    val campus = filters.asStringList("campus")
    campus.forEach { formParts.add("campusFilter%5B%5D=${it.encode()}") }

    val subjects = filters.asStringList("subject")
    subjects.forEach { formParts.add("subject%5B%5D=${it.encode()}") }

    formParts.add("courseNumber=${(filters["courseNum"] as? String ?: "").encode()}")
    formParts.add("openWaitlist=${(filters["openWaitlist"] as? String ?: "").encode()}")
    formParts.add("crnNumber=${filters["crn"]?.toString()?.encode() ?: ""}")
    formParts.add("level=${(filters["level"] as? String ?: "").encode()}")
    formParts.add("courseTitle=${(filters["courseTitle"] as? String ?: "").encode()}")
    formParts.add("instructor=")
    formParts.add("instructorUid=")
    formParts.add("creditHours=${filters["creditHours"]?.toString()?.encode() ?: ""}")

    val startEndTime = filters.asStringList("startEndTime")
    if (startEndTime.isNotEmpty()) {
        startEndTime.forEach { formParts.add("startEndTime%5B%5D=${it.encode()}") }
    } else {
        formParts.addAll(listOf("startEndTime%5B%5D=", "startEndTime%5B%5D="))
    }

    formParts.add("courseSearch=Find")

    val delivery = filters.asStringList("delivery")
    delivery.forEach { formParts.add("sectionAttributes%5B%5D=${it.encode()}") }

    val attributes = filters.asStringList("attributes")
    attributes.forEach { formParts.add("sectionFilterAttributes%5B%5D=${it.encode()}") }

    val partOfTerm = filters.asStringList("partOfTerm")
    partOfTerm.forEach { formParts.add("partOfTerm%5B%5D=${it.encode()}") }

    val daysFilter = filters.asStringList("daysFilter")
    daysFilter.forEach { formParts.add("daysFilter%5B%5D=${it.encode()}") }

    return formParts
}

/**
 * Safely extracts a list of strings from a JSON map value that could be
 * a List<String>, List<Any>, or null.
 */
private fun Map<String, Any?>.asStringList(key: String): List<String> {
    val value = this[key] ?: return emptyList()
    return when (value) {
        is List<*> -> value
            .filterNotNull()
            .map { it.toString() }
        is String -> listOf(value)
        else -> emptyList()
    }
}
