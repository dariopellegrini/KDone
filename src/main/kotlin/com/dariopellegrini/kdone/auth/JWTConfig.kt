package com.dariopellegrini.kdone.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

class JWTConfig(private val secret: String,
                private val issuer: String = "kdone.com",
                private val validityInMs: Long? = null, // 100 hours
                private val algorithm: Algorithm = Algorithm.HMAC512(secret)) {

    val verifier: JWTVerifier = JWT
            .require(algorithm)
            .withIssuer(issuer)
            .build()

    /**
     * Produce a token for this combination of UserAuth and Account
     */
    fun makeToken(userAuth: UserAuth): String = JWT.create()
        .withSubject(userAuth.userId)
        .withIssuer(issuer)
        .withClaim("role", userAuth.role)
        .withClaim("time", Date().time)
        .apply { if (validityInMs != null) {
            /**
             * Calculate the expiration Date based on current time + the given validity
             */
            withExpiresAt(Date(System.currentTimeMillis() + validityInMs))
        } }
        .sign(algorithm)
}
