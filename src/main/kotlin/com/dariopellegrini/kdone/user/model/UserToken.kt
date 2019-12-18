package com.dariopellegrini.kdone.user.model

import org.litote.kmongo.Id
import java.util.*

data class UserToken(val userId: Id<KDoneUser>,
                     val token: String,
                     val dateCreated: Date)