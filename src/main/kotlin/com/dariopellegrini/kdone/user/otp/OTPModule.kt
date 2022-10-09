package com.dariopellegrini.kdone.user.otp

import com.dariopellegrini.kdone.application.mongoRepository
import com.dariopellegrini.kdone.auth.*
import com.dariopellegrini.kdone.constants.otpTokensCollection
import com.dariopellegrini.kdone.exceptions.*
import com.dariopellegrini.kdone.extensions.*
import com.dariopellegrini.kdone.mongo.MongoRepository
import com.dariopellegrini.kdone.user.UserRouteConfiguration
import com.dariopellegrini.kdone.user.model.KDoneUser
import com.dariopellegrini.kdone.user.model.UserToken
import com.dariopellegrini.kdone.user.model.UsernameInput
import com.dariopellegrini.kdone.utils.randomString
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import io.ktor.server.application.call
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.isMultipart
import io.ktor.server.request.receive
import  io.ktor.server.response.header
import  io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.litote.kmongo.eq
import java.util.*

inline fun <reified T : KDoneUser> Route.otpModule(endpoint: String = "users",
                                                   repository: MongoRepository<T>,
                                                   tokenRepository: MongoRepository<UserToken>,
                                                   jwtConfig: JWTConfig,
                                                   configuration: UserRouteConfiguration<T>) {
    val otpTokensRepository = mongoRepository<OTPToken>(otpTokensCollection)
    val senders = configuration.otpSenders ?: throw ServerException(500, "OPT sender missing")

    if (senders.isEmpty()) {
        logger.warn("No OTP sender configured")
    }

    val codeGenerator = configuration.otpCodeGenerator ?: {
        randomString(5)
    }

    // For new or already registered users
    post("$endpoint/otp/access") {
        try {

            val checkInput: suspend (String?, String?) -> Unit = {
                    _, role ->
                if (!configuration.authorization.check(guest, create, role ?: registered.rawValue)) throw ForbiddenException()
                role?.let {
                    if (!configuration.authorization.check(guest, create, it)) throw ForbiddenException()
                }
            }

            if (call.request.isMultipart()) {
                val uploader = configuration.uploader ?: throw ServerException(500, "Uploader not configured")
                var registeredUser: T? = null
                try {
                    val mapRequest = call.receiveMultipartMap<T>(uploader) {
                        val username = it["username"] as? String ?: throw BadRequestException("Missing username")
                        registeredUser = repository.findOneOrNull(KDoneUser::username eq username)
                        if (registeredUser != null) {
                            throw AlreadyRegistered(username)
                        } else {
                            if (it.containsKey("password")) throw ForbiddenException("Password not allowed")
                            checkInput(it["username"] as? String, it["role"] as? String)
                        }
                    }
                    val user = ObjectMapper().configureForKDone().convertValue<T>(mapRequest)
                    user.otp = true
                    repository.insert(user)
                    val otpToken = OTPToken(codeGenerator()).apply { owner = user._id }
                    otpTokensRepository.insert(otpToken)
                    call.respond(HttpStatusCode.Created, mapOf("username" to user.username))
                } catch (t: AlreadyRegistered) {
                    val user = (registeredUser ?: repository.findOneOrNull(KDoneUser::username eq t.username))
                        ?: throw ServerException(500, "User not found even if found before")
                    if (user.otp != true) throw ForbiddenException("OTP not enabled for this user")
                    otpTokensRepository.deleteMany(OTPToken::owner eq user._id)
                    val otpToken = OTPToken(codeGenerator()).apply { owner = user._id }
                    otpTokensRepository.insert(otpToken)

                    // Send
                    senders.map {
                        async {
                            it.send(user, otpToken.password)
                        }
                    }.awaitAll()

                    call.respond(HttpStatusCode.OK, mapOf("username" to user.username))
                }
            } else {
                val mapRequest = call.receiveMap<T>()
                val username = mapRequest["username"] as? String ?: throw BadRequestException("Missing username")
                val registeredUser = repository.findOneOrNull(KDoneUser::username eq username)
                if (registeredUser != null) {
                    if (registeredUser.otp != true) throw ForbiddenException("OTP not enabled for this user")
                    otpTokensRepository.deleteMany(OTPToken::owner eq registeredUser._id)
                    val otpToken = OTPToken(codeGenerator()).apply { owner = registeredUser._id }
                    otpTokensRepository.insert(otpToken)
                    call.respond(HttpStatusCode.OK, mapOf("username" to registeredUser.username))
                } else {
                    if (mapRequest.containsKey("password")) throw ForbiddenException("Cannot change password")
                    checkInput(username, mapRequest["role"] as? String)

                    val user = ObjectMapper().configureForKDone().convertValue<T>(mapRequest)
                    user.otp = true
                    repository.insert(user)
                    val otpToken = OTPToken(codeGenerator()).apply { owner = user._id }
                    otpTokensRepository.insert(otpToken)

                    // Send
                    senders.map {
                        async {
                            it.send(user, otpToken.password)
                        }
                    }.awaitAll()

                    call.respond(HttpStatusCode.Created, mapOf("username" to user.username))
                }
            }
        } catch (e: Exception) {
            call.respondWithException(e)
        }
    }

    // For already registered users
    post("$endpoint/otp/access/registered") {
        try {
            val input = call.receive<UsernameInput>()
            val user = repository.findOneOrNull(KDoneUser::username eq input.username) ?: throw NotFoundException("User not found")
            if (user.otp != true) throw ForbiddenException("OTP not enabled for this user")
            otpTokensRepository.deleteMany(OTPToken::owner eq user._id)
            val otpToken = OTPToken(codeGenerator()).apply { owner = user._id }
            otpTokensRepository.insert(otpToken)

            // Send
            senders.map {
                async {
                    it.send(user, otpToken.password)
                }
            }.awaitAll()

            call.respond(HttpStatusCode.OK, mapOf("username" to user.username))
        } catch (e: Exception) {
            call.respondWithException(e)
        }
    }

    // OTP login
    post("$endpoint/otp/login") {
        try {
            if (configuration.loginDisabled) throw ForbiddenException("Forbidden operation")

            val input = call.receive<OTPLoginInput>()
            val user = repository.findOneOrNull(
                KDoneUser::username eq input.username) ?: throw NotAuthorizedException()
            val otpToken = otpTokensRepository.findOneOrNull(
                OTPToken::password eq input.password,
                OTPToken::owner eq user._id) ?: throw NotAuthorizedException()

            val token = jwtConfig.makeToken(UserAuth(user._id.toString(), user.role))
            call.response.header(HttpHeaders.Authorization, token)
            tokenRepository.insert(UserToken(user._id, token, Date()))

            otpTokensRepository.deleteById(otpToken._id.cast())

            call.respond(HttpStatusCode.OK, user.secure())
        } catch (e: Exception) {
            call.respondWithException(e)
            configuration.exceptionHandler?.invoke(call, e)
        }
    }
}

data class AlreadyRegistered(val username: String): Throwable()