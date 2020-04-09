package com.dariopellegrini.kdone.extensions

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun Any.secure(): Map<String, Any> {

    val mapper = ObjectMapper().configureForKDone()
    val jsonString = mapper.writeValueAsString(this)

    return mapper.readValue<Map<String, Any>>(jsonString).toMutableMap().apply {
        remove("password")

        remove("facebookId")
        remove("appleId")
        remove("googleId")
    }
}

val Any.logger: Logger get() = LoggerFactory.getLogger(this::class.java)
