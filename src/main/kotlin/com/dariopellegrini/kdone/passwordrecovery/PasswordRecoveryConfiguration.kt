package com.dariopellegrini.kdone.passwordrecovery

import com.dariopellegrini.kdone.email.EmailClient
import com.dariopellegrini.kdone.email.model.EmailMessage

class PasswordRecoveryConfiguration(val emailSender: EmailClient,
                                    val baseURL: String,
                                    val redirectURL: String? = null,
                                    val emailSenderClosure: (String) -> EmailMessage)