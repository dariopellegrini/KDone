package com.dariopellegrini.kdone.user.social.apple

import auth.userAuthOrNull
import com.auth0.jwk.UrlJwkProvider
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.RSAKeyProvider
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.litote.kmongo.eq
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.*

inline fun <reified T: KDoneUser>Route.apple(bundleId: String,
                   repository: MongoRepository<T>,
                   tokenRepository: MongoRepository<UserToken>,
                   jwtConfig: JWTConfig,
                   configuration: UserRouteConfiguration<T>) {

    authenticate("jwt", optional = true) {
        post("apple/access") {
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

                val appleToken = call.request.headers["appleToken"] ?: throw BadRequestException("Missing apple token")
                val appleId = call.request.headers["appleId"] ?: throw BadRequestException("Missing apple id")

                checkAppleToken(appleToken, appleId, bundleId)

                val existingUser = repository.findOneOrNull(KDoneUser::appleId eq appleId)
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
                    input.appleId = appleId
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

suspend fun checkAppleToken(appleToken: String, appleId: String, bundleId: String) = withContext(Dispatchers.IO) {
    val keyProvider = object : RSAKeyProvider {
        override fun getPrivateKeyId(): String? {
            return null
        }

        override fun getPrivateKey(): RSAPrivateKey? {
            return null
        }

        override fun getPublicKeyById(kid: String): RSAPublicKey {
            val provider = UrlJwkProvider("https://appleid.apple.com/auth/keys/")
            val jwk = provider.all.first()
            return jwk.publicKey as RSAPublicKey
        }
    }

    val verifier = JWT.require(Algorithm.RSA256(keyProvider))
        .build()
    val res = verifier.verify(appleToken)

    val claimAppleId = res.subject
    val claimBundleId = res.getClaim("aud").asString()

    if (claimAppleId != appleId) throw ForbiddenException("Apple ID is incorrect with this token")
    if (claimBundleId != bundleId) throw ForbiddenException("Bundle ID is incorrect with this token")
}
