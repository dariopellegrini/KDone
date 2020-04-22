package com.dariopellegrini.kdone.email

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.simplejavamail.api.mailer.config.TransportStrategy
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.mailer.MailerBuilder
import org.simplejavamail.mailer.internal.MailerRegularBuilderImpl


class EmailSender(val host: String,
                  val username: String,
                  val password: String,
                  val port: Int = 465,
                  val buildConfiguration: (MailerRegularBuilderImpl.() -> Unit)? = null) {

    private val mailer =  MailerBuilder
        .withSMTPServer(host, port, username, password)
        .withTransportStrategy(TransportStrategy.SMTPS)
        .apply {
            buildConfiguration?.let { this.it() }
        }
        .buildMailer()

    suspend fun send(sender: Pair<String, String>, recipient: Pair<String, String>, subject: String, content: String) = withContext(
        Dispatchers.IO) {
        val email = EmailBuilder.startingBlank()
            .from(sender.first, sender.second)
            .to(recipient.first, recipient.second)
            .withSubject(subject)
            .withPlainText(content)
            .buildEmail()

        mailer.sendMail(email)
    }
}

fun smtpClient(host: String,
               username: String,
               password: String,
               port: Int = 465,
               buildConfiguration: (MailerRegularBuilderImpl.() -> Unit)? = null): EmailSender {
    return EmailSender(host, username, password, port, buildConfiguration)
}
