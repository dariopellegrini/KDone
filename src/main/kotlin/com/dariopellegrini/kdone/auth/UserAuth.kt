package com.dariopellegrini.kdone.auth

import io.ktor.auth.Principal

data class UserAuth(
        val userId: String,
        val role: String?) : Principal