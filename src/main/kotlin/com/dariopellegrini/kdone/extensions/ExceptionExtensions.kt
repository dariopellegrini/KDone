package com.dariopellegrini.kdone.extensions

import com.auth0.jwt.exceptions.SignatureVerificationException
import com.auth0.jwt.exceptions.TokenExpiredException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.dariopellegrini.kdone.exceptions.*
import io.ktor.application.ApplicationCall
import io.ktor.features.UnsupportedMediaTypeException
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import kotlinx.io.errors.IOException
import org.bson.json.JsonParseException
import java.lang.IllegalArgumentException

suspend fun ApplicationCall.respondWithException(e: Exception) {
    println("$e\n${e.localizedMessage}")
    when (e) {
        is UserIdNotVerifiedForDocument -> respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to e.localizedMessage, "type" to "Authorization error"))
        is NotAuthorizedException -> respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to e.localizedMessage, "type" to "Authorization error"))
        is SignUpErrorException -> respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to e.localizedMessage, "type" to "Authorization error"))
        is MissingDocumentId -> respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to e.localizedMessage, "type" to "Request error"))
        is MismatchedInputException -> respond(
            HttpStatusCode.BadRequest,
            mapOf("error" to e.localizedMessage, "type" to "Input error"))
        is JsonMappingException -> respond(
            HttpStatusCode.BadRequest,
            mapOf("error" to e.localizedMessage, "type" to "Input error"))
//        is JsonSyntaxException -> respond(
//                HttpStatusCode.BadRequest,
//                mapOf("error" to e.localizedMessage, "type" to "Request error"))
        is MissingAccessToken -> respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to e.localizedMessage, "type" to "Request error"))
        is MissingRefreshToken -> respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to e.localizedMessage, "type" to "Request error"))
        is MissingUsername -> respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to e.localizedMessage, "type" to "Request error"))
        is MissingPassword -> respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to e.localizedMessage, "type" to "Request error"))
        is MissingMotherInfoId -> respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to e.localizedMessage, "type" to "Request error"))
        is MissingSonInfoId -> respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to e.localizedMessage, "type" to "Request error"))
        is DocumentNotFound -> respond(
                HttpStatusCode.NotFound,
                mapOf("error" to e.localizedMessage, "type" to "Resource not found"))
        is MotherInfoIdNotFound -> respond(
                HttpStatusCode.NotFound,
                mapOf("error" to e.localizedMessage, "type" to "Resource not found"))
        is SonInfoIdNotFound -> respond(
                HttpStatusCode.NotFound,
                mapOf("error" to e.localizedMessage, "type" to "Resource not found"))
        is AuthNotValidException -> respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to e.localizedMessage, "type" to "Signup error"))
        is ServerException -> respond(
            HttpStatusCode(e.statusCode, e.localizedMessage),
            mapOf("error" to e.localizedMessage))
        is NotFoundException -> respond(
            HttpStatusCode.NotFound,
            mapOf("error" to e.localizedMessage)
        )
        is ForbiddenException -> respond(
            HttpStatusCode.Forbidden,
            mapOf("error" to e.localizedMessage, "type" to "Forbidden error"))
        is UsernameAlreadyExists -> respond(
            HttpStatusCode.Conflict,
            mapOf("error" to e.localizedMessage, "type" to "Conflict error"))
        is IllegalArgumentException -> {
            if (e.localizedMessage.contains("ObjectId")) respond( // Mongo ID not found
                HttpStatusCode.NotFound,
                mapOf("error" to e.localizedMessage)
            )
            else respond(
                HttpStatusCode(400, "Input error"),
                mapOf("error" to e.localizedMessage))
        }
        is UnsupportedMediaTypeException -> respond(
            HttpStatusCode(HttpStatusCode.BadRequest.value, "Bad request"),
            mapOf("error" to e.localizedMessage))
        is IOException -> respond(
            HttpStatusCode(HttpStatusCode.BadRequest.value, "Bad request"),
            mapOf("error" to e.localizedMessage))
        is EnumValueException -> respond(
            HttpStatusCode(HttpStatusCode.BadRequest.value, "Bad request"),
            mapOf("error" to e.text))
        is BadRequestException -> respond(
            HttpStatusCode(HttpStatusCode.BadRequest.value, e.localizedMessage),
            mapOf("error" to e.localizedMessage))
        is TokenExpiredException -> respond(
            HttpStatusCode(HttpStatusCode.Unauthorized.value, "Not authorized"),
            mapOf("error" to e.localizedMessage))
        is SignatureVerificationException -> respond(
            HttpStatusCode(HttpStatusCode.Unauthorized.value, "Invalid signature"),
            mapOf("error" to "Invalid signature"))
        is JsonParseException -> respond(
            HttpStatusCode(HttpStatusCode.BadRequest.value, "Bad request"),
            mapOf("error" to e.localizedMessage))
        else -> respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to e.localizedMessage, "type" to "Generic error", "exception" to e.toString()))
    }
}
