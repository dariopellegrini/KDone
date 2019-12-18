package com.dariopellegrini.kdone.exceptions

class ServerException(val statusCode: Int, val text: String): Exception() {
    override fun getLocalizedMessage(): String {
        return text
    }
}