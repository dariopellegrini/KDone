package com.dariopellegrini.kdone.email.model

data class EmailMessage(val sender: EmailActor, val subject: String, val message: String)

fun email(senderName: String, senderAddress: String, subject: String, message: String): EmailMessage {
    return EmailMessage(EmailActor(senderName, senderAddress), subject, message)
}