package com.dariopellegrini.kdone.exceptions

class NotAuthorizedException(val text: String? = null): Exception() {
    override fun getLocalizedMessage(): String {
        return text ?: "Not authorized"
    }
}