package com.dariopellegrini.kdone.exceptions

class BadRequestException(val text: String? = null): Exception() {
    override fun getLocalizedMessage(): String {
        return text ?: "Bad request"
    }
}