package com.dariopellegrini.kdone.extensions

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

fun Any.secure(): Map<String, Any> {
    val mapper = ObjectMapper().configureForKDone()
    val jsonString = mapper.writeValueAsString(this)
    return mapper.readValue<Map<String, Any>>(jsonString).toMutableMap().apply {
        remove("password")
        remove("facebookId")
        remove("appleId")
    }
}