package com.dariopellegrini.kdone.model

import com.dariopellegrini.kdone.user.model.KDoneUser
import org.litote.kmongo.Id
import org.litote.kmongo.newId

abstract class Identifiable {
    open val _id: Id<Any> = newId()
    open var owner: Id<KDoneUser>? = null
}