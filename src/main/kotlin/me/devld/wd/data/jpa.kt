package me.devld.wd.data

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import javax.persistence.AttributeConverter
import javax.persistence.Converter

private val OBJECT_MAPPER = ObjectMapper()

@Converter
class StringListConverter : AttributeConverter<MutableList<String>?, String?> {
    override fun convertToDatabaseColumn(attribute: MutableList<String>?): String? =
        attribute?.let { OBJECT_MAPPER.writeValueAsString(attribute) }

    override fun convertToEntityAttribute(dbData: String?): MutableList<String>? =
        dbData?.let { OBJECT_MAPPER.readValue<MutableList<String>>(dbData) }
}

@Converter
class LongListConverter : AttributeConverter<MutableList<Long>?, String?> {
    override fun convertToDatabaseColumn(attribute: MutableList<Long>?): String? =
        attribute?.let { attribute.joinToString(",") }

    override fun convertToEntityAttribute(dbData: String?): MutableList<Long>? =
        dbData?.let { dbData.split(",").filter { it.isNotEmpty() }.map { it.toLong() }.toMutableList() }
}
