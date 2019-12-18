package com.dariopellegrini.kdone.exceptions

class AuthNotValidException: Exception() {
    override fun getLocalizedMessage(): String {
        return "User auth is not valid"
    }
}