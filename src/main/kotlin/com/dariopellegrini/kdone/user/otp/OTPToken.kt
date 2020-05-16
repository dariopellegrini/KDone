package com.dariopellegrini.kdone.user.otp

import com.dariopellegrini.kdone.model.DateModel
import com.dariopellegrini.kdone.model.Identifiable
import java.util.*

data class OTPToken(val password: String,
                    override var dateCreated: Date = Date(),
                    override var dateUpdated: Date = Date()): Identifiable(), DateModel