package com.dariopellegrini.kdone.exceptions

import java.lang.Exception

class MissingMotherInfoId: Exception() {
    override fun getLocalizedMessage(): String {
        return "Missing motherInfoId"
    }
}