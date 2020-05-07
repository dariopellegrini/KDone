package com.dariopellegrini.kdone.passwordrecovery

import com.dariopellegrini.kdone.email.EmailSender
import com.dariopellegrini.kdone.email.model.EmailMessage

class PasswordRecoveryConfiguration(val emailSender: EmailSender,
                                    val baseURL: String,
                                    val redirectURL: String? = null,
                                    val emailSenderClosure: (String) -> EmailMessage)