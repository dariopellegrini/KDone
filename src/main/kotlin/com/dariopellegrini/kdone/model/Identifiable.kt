package com.dariopellegrini.kdone.model

import com.dariopellegrini.kdone.user.model.KDoneUser
import org.litote.kmongo.Id
import org.litote.kmongo.newId

abstract class Identifiable {
    var _id: Id<Any> = newId()
    var owner: Id<KDoneUser>? = null
}