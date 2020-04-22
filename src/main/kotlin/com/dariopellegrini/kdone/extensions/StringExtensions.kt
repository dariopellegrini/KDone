package com.dariopellegrini.kdone.extensions

import java.net.URI
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.regex.Pattern

val String.isEmailValid: Boolean
    get() {
        return Pattern.compile(
            "^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]|[\\w-]{2,}))@"
                    + "((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                    + "[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\."
                    + "([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                    + "[0-9]{1,2}|25[0-5]|2[0-4][0-9]))|"
                    + "([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,4})$"
        ).matcher(this).matches()
    }

val String.date: Date
    get() {
        val tz = TimeZone.getTimeZone("UTC")
        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss'Z'")
        df.timeZone = tz
        return df.parse(this)
    }

val String.dateOrNull: Date?
    get() {
        return try {
            val tz = TimeZone.getTimeZone("UTC")
            val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss'Z'")
            df.timeZone = tz
            return df.parse(this)
        } catch (e: Exception) {
            null
        }
    }

val String.normalizeURL get() = URI(this).normalize().toString()