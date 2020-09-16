package com.dariopellegrini.kdone.exceptions

import java.lang.Exception

class MissingPrivacyException: Exception() {
    override fun getLocalizedMessage(): String {
        return "Privacy not present for this user"
    }
}