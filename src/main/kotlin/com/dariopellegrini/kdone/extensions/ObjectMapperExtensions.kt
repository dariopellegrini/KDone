package com.dariopellegrini.kdone.extensions

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.litote.kmongo.id.jackson.IdJacksonModule
import java.text.SimpleDateFormat

fun ObjectMapper.configureForKDone(): ObjectMapper {
    enable(SerializationFeature.INDENT_OUTPUT)
    dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss'Z'")
    registerModule(IdJacksonModule())
    registerKotlinModule()
    return this
 }