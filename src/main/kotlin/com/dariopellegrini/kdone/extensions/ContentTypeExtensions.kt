package com.dariopellegrini.kdone.extensions

import com.premionocivelli.utils.guardLet
import io.ktor.http.ContentType

val ContentType?.contentTypeString: String
    get() {
        val (contentType, contentSubtype) = guardLet(this?.contentType, this?.contentSubtype) { return "application/binary" }
        return "$contentType/$contentSubtype"
    }