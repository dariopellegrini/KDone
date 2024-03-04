package com.dariopellegrini.kdone.extensions

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.util.StdDateFormat
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.litote.kmongo.id.jackson.IdJacksonModule
import java.text.SimpleDateFormat
import java.util.*

fun ObjectMapper.configureForKDone(): ObjectMapper {
    enable(SerializationFeature.INDENT_OUTPUT)
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    this.dateFormat = dateFormat
    registerModule(IdJacksonModule())
    registerKotlinModule()
    return this
 }