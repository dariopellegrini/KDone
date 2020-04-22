package com.dariopellegrini.kdone.user.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.litote.kmongo.Id
import org.litote.kmongo.newId

abstract class KDoneUser {
    abstract var username: String
    open var password: String? = null
    val _id: Id<KDoneUser> = newId()

    open var role: String? = null

    open var confirmed: Boolean? = null

    open var facebookId: String? = null
    open var appleId: String? = null
    open var googleId: String? = null
}

val ownerForbiddenAttributes = listOf("_id", "password", "role", "confirmed", "facebookId", "appleId", "googleId")

data class LoginInput(val username: String, val password: String)

//
@JsonIgnoreProperties(ignoreUnknown = true)
data class UsernameInput(val username: String)