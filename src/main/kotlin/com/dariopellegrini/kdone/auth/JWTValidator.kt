package com.dariopellegrini.kdone.auth

import io.ktor.auth.jwt.JWTCredential

interface Validator {
    fun validate(credentials: JWTCredential): UserAuth?
}

class JWTValidator: Validator {

    // Authentication with user and password
    override fun validate(credentials: JWTCredential): UserAuth? {
        // JWT decifrato
        val id = credentials.payload.subject
        val role = credentials.payload.getClaim("role").asString()
        return UserAuth(id, role)
    }
}