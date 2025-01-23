package com.dariopellegrini.kdone.passwordrecovery

import com.dariopellegrini.kdone.email.EmailSenderInterface
import com.dariopellegrini.kdone.email.model.EmailMessage

class PasswordRecoveryConfiguration(val emailSender: EmailSenderInterface,
                                    val baseURL: String,
                                    val redirectURL: String? = null,
                                    val emailSenderClosure: (String) -> EmailMessage)