package com.dariopellegrini.kdone.exceptions

import java.lang.Exception

class MissingAccessToken: Exception() {
    override fun getLocalizedMessage(): String {
        return "Missing access token"
    }
}