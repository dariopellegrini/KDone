package com.dariopellegrini.kdone.user.social.facebook

import auth.userAuthOrNull
import com.dariopellegrini.kdone.application.database
import com.dariopellegrini.kdone.auth.*
import com.dariopellegrini.kdone.exceptions.*
import com.dariopellegrini.kdone.extensions.*
import com.dariopellegrini.kdone.mongo.MongoRepository
import com.dariopellegrini.kdone.user.UserRouteConfiguration
import com.dariopellegrini.kdone.user.model.KDoneUser
import com.dariopellegrini.kdone.user.model.UserToken
import com.dariopellegrini.kdone.user.model.UsernameInput
import com.dariopellegrini.kdone.utilities.NetworkUtilities
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
import org.litote.kmongo.eq
import java.util.*

inline fun <reified T: KDoneUser>Route.facebook(appId: String,
                   appSecret: String,
                   repository: MongoRepository<T>,
                   tokenRepository: MongoRepository<UserToken>,
                   jwtConfig: JWTConfig,
                   configuration: UserRouteConfiguration<T>) {

    authenticate("jwt", optional = true) {
        post("facebook/access") {
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

                val facebookToken = call.request.headers["facebookToken"] ?: throw BadRequestException("Missing Facebook token")
                val facebookId = call.request.headers["facebookId"] ?: throw BadRequestException("Missing Facebook id")

                // Check with Facebook
                val endpoint = "https://graph.facebook.com/debug_token?input_token=$facebookToken&access_token=$appId|$appSecret"
                val result = NetworkUtilities.executeGet(endpoint)
                val facebookResult = ObjectMapper().configureForKDone().readValue<FacebookGraphResult>(result)
                if (facebookResult.data.app_id == null || facebookResult.data.error != null)
                    throw ForbiddenException(facebookResult.data.error?.message ?: "Facebook error")
                if (facebookResult.data.user_id != facebookId) throw ForbiddenException("Facebook ID not allowed to perform this operation")

                val existingUser = repository.findOneOrNull(KDoneUser::facebookId eq facebookId)

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
                    input.facebookId = facebookId

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