package com.dariopellegrini.kdone.exceptions

import java.lang.Exception

class MissingUsername: Exception() {
    override fun getLocalizedMessage(): String {
        return "Missing username"
    }
}