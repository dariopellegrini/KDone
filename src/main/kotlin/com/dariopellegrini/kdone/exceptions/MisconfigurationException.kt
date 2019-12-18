package com.dariopellegrini.kdone.exceptions

class MisconfigurationException(val text: String): Exception() {
    override fun getLocalizedMessage(): String {
        return text
    }
}