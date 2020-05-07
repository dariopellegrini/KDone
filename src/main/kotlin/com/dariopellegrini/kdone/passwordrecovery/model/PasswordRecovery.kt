package com.dariopellegrini.kdone.passwordrecovery.model

import com.dariopellegrini.kdone.model.DateModel
import com.dariopellegrini.kdone.user.model.KDoneUser
import org.litote.kmongo.Id
import org.litote.kmongo.newId
import java.util.*

class PasswordRecovery(val userId: Id<KDoneUser>,
                       val username: String,
                       val code: String,
                       val newPassword: String,
                       override var dateCreated: Date = Date(),
                       override var dateUpdated: Date= Date()): DateModel {
    val _id: Id<PasswordRecovery> = newId()
    var active = true
}
