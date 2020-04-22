package com.dariopellegrini.kdone.email.model

data class EmailUser(val name: String, val address: String)

fun sender(name: String, address: String): EmailUser {
    return EmailUser(name, address)
}