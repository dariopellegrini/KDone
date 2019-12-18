package com.dariopellegrini.kdone.exceptions

import java.lang.Exception

class MissingRefreshToken: Exception() {
    override fun getLocalizedMessage(): String {
        return "Missing refresh token"
    }
}