package com.dariopellegrini.kdone.extensions

import java.text.SimpleDateFormat
import java.util.*


val Date.ISOUTC: String
    get() {
        val tz = TimeZone.getTimeZone("UTC")
        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss'Z'")
        df.timeZone = tz
        return df.format(this)
    }

fun Date.yearsFromNow(): Double {
    val milliseconds = this.time
    val currentMilliseconds = Date().time
    val difference = currentMilliseconds - milliseconds

    val years = (difference.toDouble() / 1000 / 60 / 60 / 24 / 365.25)

    return years
}