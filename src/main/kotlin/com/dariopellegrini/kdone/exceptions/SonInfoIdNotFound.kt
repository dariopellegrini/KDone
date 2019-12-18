package com.dariopellegrini.kdone.exceptions

import java.lang.Exception

class SonInfoIdNotFound: Exception() {
    override fun getLocalizedMessage(): String {
        return "Requested sonInfoId not found"
    }
}