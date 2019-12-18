package com.dariopellegrini.kdone.extensions

import java.util.*
import java.util.Calendar
import java.util.GregorianCalendar



fun Date.yearsFromNow(): Double {
    val milliseconds = this.time
    val currentMilliseconds = Date().time
    val difference = currentMilliseconds - milliseconds

    val years = (difference.toDouble() / 1000 / 60 / 60 / 24 / 365.25)

    return years
}
