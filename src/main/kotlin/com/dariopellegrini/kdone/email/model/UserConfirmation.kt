package com.dariopellegrini.kdone.email.model

import com.dariopellegrini.kdone.model.DateModel
import com.dariopellegrini.kdone.user.model.KDoneUser
import org.litote.kmongo.Id
import org.litote.kmongo.newId
import java.util.*

class UserConfirmation(val userId: Id<KDoneUser>,
                          val username: String,
                          val code: String,
                          var confirmed: Boolean,
                          override var dateCreated: Date = Date(),
                          override var dateUpdated: Date= Date()): DateModel {
    val _id: Id<Any> = newId()
}