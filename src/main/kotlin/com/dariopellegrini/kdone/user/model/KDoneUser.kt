package com.dariopellegrini.kdone.user.model

import org.litote.kmongo.Id
import org.litote.kmongo.newId

abstract class KDoneUser {
    abstract var username: String
    abstract var password: String
    var _id: Id<KDoneUser> = newId()

    var role: String? = null
}

data class LoginInput(val username: String, val password: String)