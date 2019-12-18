package com.dariopellegrini.kdone.exceptions

import java.lang.Exception

class MissingDocumentId: Exception() {
    override fun getLocalizedMessage(): String {
        return "Missing document userId"
    }
}