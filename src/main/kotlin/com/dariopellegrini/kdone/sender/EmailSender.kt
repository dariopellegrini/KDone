package com.dariopellegrini.kdone.sender

import com.dariopellegrini.kdone.email.EmailClient
import com.dariopellegrini.kdone.user.model.KDoneUser

class EmailSender<T: KDoneUser>(private val emailClient: EmailClient,
                                private val sender: Pair<String, String>,
                                private val subject: String,
                                private val content: (String) -> String): UserSender<T> {

    override suspend fun send(user: T, message: String) {
        emailClient.send(sender, (user.username to user.username), subject, content.invoke(message))
    }
}
