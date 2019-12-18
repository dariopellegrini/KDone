package com.dariopellegrini.kdone.exceptions

class UserIdNotVerifiedForDocument: Exception() {
    override fun getLocalizedMessage(): String {
        return "Not authorized to perform this action"
    }
}