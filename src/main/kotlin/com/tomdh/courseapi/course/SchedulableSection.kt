package com.tomdh.courseapi.course

/**
 * Universal representation of a course section.
 * Every SchoolConnector must translate their school-specific data into this.
 *
 * The scheduling combinator only needs [timeWindows] for conflict detection.
 * [name] is for display. [data] preserves the school-specific blob for round-tripping.
 */
data class SchedulableSection(
    /** Display name (e.g. "CSE 271 - Object-Oriented Programming") */
    val name: String,

    /** Parsed time windows in canonical format */
    val timeWindows: List<CanonicalTimeWindow>,

    /** The original school-specific data, preserved as a JSON map */
    val data: Map<String, Any?>
)

/**
 * A single time block on a specific day, in a standardized format
 * that every school connector normalizes to.
 */
data class CanonicalTimeWindow(
    /** Day of week name (e.g. "MONDAY", "TUESDAY") */
    val day: String,

    /** Normalized start time (e.g. "10:00am") */
    val startTime: String,

    /** Normalized end time (e.g. "10:50am") */
    val endTime: String
)
