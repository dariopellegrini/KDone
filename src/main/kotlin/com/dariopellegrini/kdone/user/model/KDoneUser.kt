package com.dariopellegrini.kdone.user.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.litote.kmongo.Id
import org.litote.kmongo.newId

abstract class KDoneUser {
    abstract var username: String
    abstract var password: String?
    val _id: Id<KDoneUser> = newId()
    var role: String? = null

    open var facebookId: String? = null
    open var appleId: String? = null
    open var googleId: String? = null
}

data class LoginInput(val username: String, val password: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class UsernameInput(val username: String)