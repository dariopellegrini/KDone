package com.dariopellegrini.kdone.exceptions

class EnumValueException(val text: String): Exception() {
    override fun getLocalizedMessage(): String {
        return text
    }
}