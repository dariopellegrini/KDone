package com.dariopellegrini.kdone.email.model

data class EmailMessage(val sender: EmailUser, val subject: String, val message: String)

fun email(senderName: String, senderAddress: String, subject: String, message: String): EmailMessage {
    return EmailMessage(EmailUser(senderName, senderAddress), subject, message)
}