package com.dariopellegrini.kdone.email

import com.dariopellegrini.kdone.email.model.EmailMessage

class EmailConfirmationConfiguration(val emailSender: EmailSender,
                         val baseURL: String,
                         val redirectURL: String? = null,
                         val emailSenderClosure: (String) -> EmailMessage)