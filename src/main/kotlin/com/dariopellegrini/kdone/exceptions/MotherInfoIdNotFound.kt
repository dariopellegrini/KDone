package com.dariopellegrini.kdone.exceptions

import java.lang.Exception

class MotherInfoIdNotFound: Exception() {
    override fun getLocalizedMessage(): String {
        return "Requested motherInfoId not found"
    }
}