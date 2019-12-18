package com.dariopellegrini.kdone.exceptions

import java.lang.Exception

class UsernameAlreadyExists: Exception() {
    override fun getLocalizedMessage(): String {
        return "Username already exists"
    }
}