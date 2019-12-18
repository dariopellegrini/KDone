package com.dariopellegrini.kdone.exceptions

class SignUpErrorException: Exception() {
    override fun getLocalizedMessage(): String {
        return "Error during signup"
    }
}