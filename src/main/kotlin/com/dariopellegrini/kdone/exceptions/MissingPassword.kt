package com.dariopellegrini.kdone.exceptions

import java.lang.Exception

class MissingPassword: Exception() {
    override fun getLocalizedMessage(): String {
        return "Missing password"
    }
}