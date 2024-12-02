package com.dariopellegrini.kdone.extensions

import com.dariopellegrini.kdone.languages.Localized
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.util.StdDateFormat
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.util.reflect.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.litote.kmongo.id.jackson.IdJacksonModule
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.coroutineContext

fun ObjectMapper.configureForKDone(): ObjectMapper {
    enable(SerializationFeature.INDENT_OUTPUT)
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    this.dateFormat = dateFormat
    registerModule(IdJacksonModule())
    registerKotlinModule()
    return this
 }

class LocalizedSerializer(val language: String) : JsonSerializer<Localized<*>>() {
    override fun serialize(
        value: Localized<*>,
        gen: JsonGenerator,
        serializers: SerializerProvider
    ) {
        val languageValue = value[language]
        if (languageValue != null) {
            // Write the value for "en"
            gen.writeObject(languageValue)
        } else {
            // Write the entire map
            gen.writeObject(value)
        }
    }
}