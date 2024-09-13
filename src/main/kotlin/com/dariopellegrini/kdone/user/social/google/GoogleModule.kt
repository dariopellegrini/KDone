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
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.litote.kmongo.eq
import java.util.*


inline fun <reified T: KDoneUser>Route.google(endpoint: String = "users",
                                              clientId: String,
                                              clientSecret: String,
                                              redirectURL: String,
                                              repository: MongoRepository<T>,
                                              tokenRepository: MongoRepository<UserToken>,
                                              jwtConfig: JWTConfig,
                                              configuration: UserRouteConfiguration<T>) {

    authenticate("jwt", optional = true) {
        post("$endpoint/google/access") {
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
        "https://accounts.google.com/o/oauth2/token",
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

suspend fun checkGoogle(clientId: String,
                        clientSecret: String,
                        redirectURL: String,
                        googleToken: String,
                        googleId: String) {
    val verifier = GoogleIdTokenVerifier.Builder(
        NetHttpTransport(),
        JacksonFactory.getDefaultInstance()
    ) // Specify the CLIENT_ID of the app that accesses the backend:
        .setAudience(Collections.singletonList(clientId)) // Or, if multiple clients access the backend:
        //.setAudience(Arrays.asList(CLIENT_ID_1, CLIENT_ID_2, CLIENT_ID_3))
        .build()

// (Receive idTokenString by HTTPS POST)


// (Receive idTokenString by HTTPS POST)
    val idToken: GoogleIdToken = verifier.verify(googleToken)
    if (idToken != null) {
        val payload: GoogleIdToken.Payload = idToken.payload

        // Print user identifier
        val userId: String = payload.getSubject()
        println("User ID: $userId")

        // Get profile information from payload
        val email: String = payload.getEmail()
        val emailVerified: Boolean = java.lang.Boolean.valueOf(payload.getEmailVerified())
        val name = payload.get("name")
        val pictureUrl = payload.get("picture")
        val locale = payload.get("locale")
        val familyName = payload.get("family_name")
        val givenName = payload.get("given_name")

        // Use or store profile information
        // ...
    } else {
        println("Invalid ID token.")
    }
}
