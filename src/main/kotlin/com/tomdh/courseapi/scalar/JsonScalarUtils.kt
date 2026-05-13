package com.tomdh.courseapi.scalar

/**
 * Safely casts an arbitrary object to a Map<String, Any?>.
 *
 * In GraphQL DataFetchers, custom scalar JSON inputs (like filters) are often parsed as
 * java.lang.Object. Directly casting this to a typed Map produces an @Suppress("UNCHECKED_CAST")
 * code smell. This utility iterates and verifies types to perform a fully safe cast.
 */
object JsonScalarUtils {
    fun safeCastToMap(value: Any?): Map<String, Any?> {
        val map = value as? Map<*, *> ?: return emptyMap()
        val result = mutableMapOf<String, Any?>()
        for ((k, v) in map) {
            if (k is String) {
                result[k] = v
            }
        }
        return result
    }
}
