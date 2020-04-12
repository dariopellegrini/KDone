package com.dariopellegrini.kdone.exceptions

class MapCheckException(val text: String): Exception() {
    override fun getLocalizedMessage(): String {
        return text
    }
}