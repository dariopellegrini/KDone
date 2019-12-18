package com.dariopellegrini.kdone.exceptions

import java.lang.Exception

class MissingSonInfoId: Exception() {
    override fun getLocalizedMessage(): String {
        return "Missing sonInfoId"
    }
}