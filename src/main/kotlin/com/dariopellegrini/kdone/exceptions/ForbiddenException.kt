package com.dariopellegrini.kdone.exceptions

class ForbiddenException(val text: String? = null): Exception() {
    override fun getLocalizedMessage(): String {
        return text ?: "Forbidden"
    }
}