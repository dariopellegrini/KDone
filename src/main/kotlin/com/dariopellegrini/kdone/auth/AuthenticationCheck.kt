package com.dariopellegrini.kdone.auth

import auth.mongoId
import auth.userAuth
import com.dariopellegrini.kdone.constants.usersTokensCollection
import com.dariopellegrini.kdone.exceptions.NotAuthorizedException
import com.dariopellegrini.kdone.user.model.UserToken
import com.mongodb.client.MongoDatabase
import io.ktor.application.ApplicationCall
import org.litote.kmongo.and
import org.litote.kmongo.eq

suspend fun ApplicationCall.checkToken(mongoDatabase: MongoDatabase) {
    val token = this.request.headers["Authorization"]?.removePrefix("Bearer ")
    if (token != null) {
        val count = mongoDatabase.getCollection(usersTokensCollection).countDocuments(
            and(UserToken::token eq token,
                UserToken::userId eq this.userAuth.userId.mongoId()))
        if (count == 0L) throw NotAuthorizedException()
    }
}

// Return should check owner
fun checkPermission(userAuth: UserAuth?, authorization: Authorization?, authEnum: AuthEnum): Boolean {
    if (authorization != null) {
        if (authorization.guest.contains(authEnum)) return false
        if (authorization.registered.contains(authEnum) && userAuth != null) return false
        if (userAuth?.role != null && authorization.roles[userAuth.role]?.contains(authEnum) == true) return false
        if (authorization.owner.contains(authEnum)) return true

        throw NotAuthorizedException()
    }
    return false
}
