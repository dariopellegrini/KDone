package com.dariopellegrini.kdone.sender

import com.dariopellegrini.kdone.user.model.KDoneUser

interface UserSender<T: KDoneUser> {
    suspend fun send(user: T, message: String)
}