package com.dariopellegrini.kdone.exceptions

class DocumentNotFound: Exception() {
    override fun getLocalizedMessage(): String {
        return "Document not found"
    }
}