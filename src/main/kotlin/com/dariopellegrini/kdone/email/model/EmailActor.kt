package com.dariopellegrini.kdone.email.model

data class EmailActor(val name: String, val address: String)

fun sender(name: String, address: String): EmailActor {
    return EmailActor(name, address)
}