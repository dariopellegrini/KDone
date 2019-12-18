package com.dariopellegrini.kdone.exceptions

class NotFoundException(val text: String? = null): Exception() {
    override fun getLocalizedMessage(): String {
        return text ?: "Not found"
    }
}