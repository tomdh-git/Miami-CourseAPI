package com.tomdh.courseapi.school.miami.parser

import com.tomdh.courseapi.school.FieldParser
import com.tomdh.courseapi.field.model.Field
import com.tomdh.courseapi.field.model.ValidFields
import org.jsoup.Jsoup
import org.springframework.stereotype.Component

@Component
class MiamiFieldParser : FieldParser {

    override fun parseTerms(raw: String): List<Field> {
        val doc = Jsoup.parse(raw)
        return doc.select("select#termFilter option[value]").map { opt ->
            Field(opt.attr("value").trim())
        }
    }

    override fun parseAllFields(raw: String): ValidFields {
        val doc = Jsoup.parse(raw)
        val subjects = doc.select("select#subject option[value]")
            .map { it.attr("value").trim() }.filter { it.isNotEmpty() }.toSet()
        val campuses = doc.select("select#campusFilter option[value]")
            .map { it.attr("value").trim() }.filter { it.isNotEmpty() }.toSet()
        val terms = doc.select("select#termFilter option[value]")
            .map { it.attr("value").trim() }.toSet()
        val deliveryTypes = doc.select("input.deliveryTypeCheckBox[value]")
            .map { it.attr("value").trim() }.filter { it.isNotEmpty() }.toSet()
        val levels = doc.select("select#levelFilter option[value]")
            .map { it.attr("value").trim() }.toSet()
        val days = doc.select("select#daysFilter option[value]")
            .map { it.attr("value").trim() }.toSet()
        val waitlistTypes = doc.select("select#openWaitlist option[value]")
            .map { it.attr("value").trim() }.toSet()
        val attributes = doc.select("select#sectionFilterAttributes option[value]")
            .map { it.attr("value").trim() }.toSet()

        return ValidFields(
            subjects = subjects,
            campuses = campuses,
            terms = terms,
            deliveryTypes = deliveryTypes,
            levels = levels,
            days = days,
            waitlistTypes = waitlistTypes,
            attributes = attributes
        )
    }
}