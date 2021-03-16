package com.dariopellegrini.kdone.websockets

import auth.userAuthOrNull
import com.dariopellegrini.kdone.application.database
import com.dariopellegrini.kdone.auth.*
import com.dariopellegrini.kdone.exceptions.NotAuthorizedException
import com.dariopellegrini.kdone.mongo.MongoRepository
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*
import org.apache.http.impl.auth.BasicScheme.authenticate
import org.litote.kmongo.json
import java.util.*
import kotlin.collections.LinkedHashSet

class WebSocketController<T: Any>(
    val repository: MongoRepository<T>,
    val endpoint: String,
    val authorization: Authorization?) {
    val sessions = Collections.synchronizedSet(LinkedHashSet<UserWebSocketSession>())

    fun configure(route: Route) {
        route.authenticate("jwt", optional = true) {
            webSocket(endpoint) { // this: DefaultWebSocketSession
                try {
                    val userAuth = call.userAuthOrNull
                    call.checkToken(this@authenticate.database)
                    val authEnum = AuthEnum.READ
                    val authorized = if (authorization != null) {
                        authorization.guest.contains(authEnum) ||
                                userAuth != null && authorization.registered.contains(authEnum) ||
                                userAuth?.role != null && authorization.roles[userAuth.role]?.contains(authEnum) == true
                    } else {
                        true
                    }
                    if (!authorized) throw NotAuthorizedException()

                    sessions += UserWebSocketSession(this, userAuth)
                    val elements = repository.findAll()
                    this.outgoing.send(Frame.Text(elements.json))
                    try {
                        while (true) {
                            when (val frame = incoming.receive()) {
                                is Frame.Text -> {
//                            val text = frame.readText()
//                            for (session in sessions) {
//                                session.connection.outgoing.send(Frame.Text(text))
//                            }
                                }
                            }
                        }
                    } finally {
                        sessions.removeIf { it.connection == this }
                    }
                } catch (e: Exception) {
                    this.outgoing.send(Frame.Text(mapOf("error" to e.localizedMessage).json))
                }
            }
        }
    }

    suspend fun update(element: T, creatorUserAuth: UserAuth?) {
        for (session in sessions) {
            val userAuth = session.userAuth
            val authEnum = AuthEnum.READ
            val authorized = if (authorization != null) {
                authorization.guest.contains(authEnum) ||
                        userAuth != null && authorization.registered.contains(authEnum) ||
                        userAuth?.role != null && authorization.roles[userAuth.role]?.contains(authEnum) == true ||
                        userAuth?.role != null && creatorUserAuth != null && authorization.roles[creatorUserAuth.role]?.contains(authEnum) == true && userAuth.role == creatorUserAuth.role
                        authorization.owner.contains(authEnum) && userAuth!= null && creatorUserAuth?.userId == userAuth.userId
            } else {
                true
            }
            if (authorized) {
                session.connection.outgoing.send(Frame.Text(element.json))
            }
        }
    }
}