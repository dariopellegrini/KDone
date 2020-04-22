package com.dariopellegrini.kdone.user.social.google

import auth.userAuthOrNull
import com.dariopellegrini.kdone.application.database
import com.dariopellegrini.kdone.auth.*
import com.dariopellegrini.kdone.exceptions.*
import com.dariopellegrini.kdone.extensions.receive
import com.dariopellegrini.kdone.extensions.respondWithException
import com.dariopellegrini.kdone.extensions.secure
import com.dariopellegrini.kdone.mongo.MongoRepository
import com.dariopellegrini.kdone.user.UserRouteConfiguration
import com.dariopellegrini.kdone.user.model.KDoneUser
import com.dariopellegrini.kdone.user.model.UserToken
import com.dariopellegrini.kdone.user.model.UsernameInput
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.request.isMultipart
import io.ktor.request.receive
import io.ktor.request.receiveMultipart
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.litote.kmongo.eq
import java.util.*


inline fun <reified T: KDoneUser>Route.google(clientId: String,
                                              clientSecret: String,
                                              redirectURL: String,
                                              repository: MongoRepository<T>,
                                              tokenRepository: MongoRepository<UserToken>,
                                              jwtConfig: JWTConfig,
                                              configuration: UserRouteConfiguration<T>) {

    authenticate("jwt", optional = true) {
        post("google/access") {
            try {
                val userAuth = call.userAuthOrNull

                if (userAuth != null) call.checkToken(this@authenticate.database)

                val checkInput: suspend (String, String?) -> Unit = {
                        username, role ->

                    if (userAuth?.role != null) {
                        if (role != null) {
                            if (!configuration.authorization.check(userAuth.role, create, role)) throw NotAuthorizedException()
                        } else {
                            if (!configuration.authorization.check(userAuth.role, create, registered)) throw NotAuthorizedException()
                        }
                    } else if (userAuth != null) {
                        if (role != null) {
                            if (!configuration.authorization.check(registered, create, role)) throw NotAuthorizedException()
                        } else {
                            if (!configuration.authorization.check(registered, create, registered)) throw NotAuthorizedException()
                        }
                    } else {
                        if (role != null) {
                            if (!configuration.authorization.check(guest, create, role)) throw NotAuthorizedException()
                        } else {
                            if (!configuration.authorization.check(guest, create, registered)) throw NotAuthorizedException()
                        }
                    }

                    if (repository.count(KDoneUser::username eq username) > 0) throw UsernameAlreadyExists()
                }

                val googleToken = call.request.headers["googleToken"] ?: throw BadRequestException("Missing googleToken")
                val googleId = call.request.headers["googleId"] ?: throw BadRequestException("Missing googleId")

                checkGoogleToken(clientId, clientSecret, redirectURL, googleToken, googleId)

                val existingUser = repository.findOneOrNull(KDoneUser::googleId eq googleId)
                if (existingUser != null) {
                    val input: UsernameInput = if (call.request.isMultipart())
                        call.receiveMultipart().receive(null)
                    else
                        call.receive()
                    if (existingUser.username != input.username) throw ForbiddenException()

                    val token = jwtConfig.makeToken(UserAuth(existingUser._id.toString(), existingUser.role))
                    call.response.header(HttpHeaders.Authorization, token)
                    tokenRepository.insert(UserToken(existingUser._id, token, Date()))

                    call.respond(HttpStatusCode.OK, existingUser.secure())
                } else {
                    val input = if (call.request.isMultipart()) {
                        val uploader = configuration.uploader ?: throw ServerException(500, "Uploader not configured")
                        call.receiveMultipart().receive<T>(uploader) {
                            if (it.containsKey("password")) throw ForbiddenException("Password not supported for Facebook access")
                            checkInput(it["username"] ?: throw BadRequestException("Missing username"), it["role"])
                        }
                    } else {
                        val input = call.receive<T>()
                        if (input.password != null) throw ForbiddenException("Password not supported for Facebook access")
                        checkInput(input.username, input.role)
                        input
                    }
                    input.googleId = googleId
                    input.confirmed = true

                    repository.insert(input)

                    val token = jwtConfig.makeToken(UserAuth(input._id.toString(), input.role))
                    call.response.header(HttpHeaders.Authorization, token)
                    tokenRepository.insert(UserToken(input._id, token, Date()))

                    call.respond(HttpStatusCode.OK, input.secure())
                }
            } catch (e: Exception) {
                call.respondWithException(e)
            }
        }
    }
}

suspend fun checkGoogleToken(clientId: String,
                             clientSecret: String,
                             redirectURL: String,
                             googleToken: String,
                             googleId: String) = withContext(Dispatchers.IO) {
    val tokenResponse = GoogleAuthorizationCodeTokenRequest(
        NetHttpTransport(),
        JacksonFactory.getDefaultInstance(),
        "https://oauth2.googleapis.com/token",
        clientId,
        clientSecret,
        googleToken,
        redirectURL
    ).execute()

    val idToken = tokenResponse.parseIdToken()
    val payload = idToken.payload
    val userId = payload.subject

    if (userId != googleId) throw ForbiddenException()
}
