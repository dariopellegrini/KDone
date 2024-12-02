package com.dariopellegrini.kdone.extensions

import com.auth0.jwt.exceptions.SignatureVerificationException
import com.auth0.jwt.exceptions.TokenExpiredException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.dariopellegrini.kdone.exceptions.*
import com.dariopellegrini.kdone.exceptions.BadRequestException
import com.dariopellegrini.kdone.exceptions.NotFoundException
import com.mongodb.MongoWriteException
import io.ktor.server.application.ApplicationCall
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.*
import  io.ktor.server.response.respond
import io.ktor.utils.io.errors.IOException
import org.bson.json.JsonParseException
import java.lang.IllegalArgumentException

suspend fun ApplicationCall.respondWithException(e: Exception) {

    logger.logger.warn("$e: ${e.localizedMessage}")
    when (e) {
        is UserIdNotVerifiedForDocument -> respond(
            HttpStatusCode.Unauthorized,
            mapOf("error" to e.completeMessage, "type" to "Authorization error"))
        is NotAuthorizedException -> respond(
            HttpStatusCode.Unauthorized,
            mapOf("error" to e.completeMessage, "type" to "Authorization error"))
        is SignUpErrorException -> respond(
            HttpStatusCode.BadRequest,
            mapOf("error" to e.completeMessage, "type" to "Authorization error"))
        is MissingDocumentId -> respond(
            HttpStatusCode.Unauthorized,
            mapOf("error" to e.completeMessage, "type" to "Request error"))
        is MismatchedInputException -> respond(
            HttpStatusCode.BadRequest,
            mapOf("error" to e.completeMessage, "type" to "Input error"))
        is JsonMappingException -> respond(
            HttpStatusCode.BadRequest,
            mapOf("error" to e.completeMessage, "type" to "Input error"))
        is MissingAccessToken -> respond(
            HttpStatusCode.BadRequest,
            mapOf("error" to e.completeMessage, "type" to "Request error"))
        is MissingRefreshToken -> respond(
            HttpStatusCode.BadRequest,
            mapOf("error" to e.completeMessage, "type" to "Request error"))
        is MissingUsername -> respond(
            HttpStatusCode.BadRequest,
            mapOf("error" to e.completeMessage, "type" to "Request error"))
        is MissingPassword -> respond(
            HttpStatusCode.BadRequest,
            mapOf("error" to e.completeMessage, "type" to "Request error"))
        is MissingMotherInfoId -> respond(
            HttpStatusCode.BadRequest,
            mapOf("error" to e.completeMessage, "type" to "Request error"))
        is MissingSonInfoId -> respond(
            HttpStatusCode.BadRequest,
            mapOf("error" to e.completeMessage, "type" to "Request error"))
        is DocumentNotFound -> respond(
            HttpStatusCode.NotFound,
            mapOf("error" to e.completeMessage, "type" to "Resource not found"))
        is MotherInfoIdNotFound -> respond(
            HttpStatusCode.NotFound,
            mapOf("error" to e.completeMessage, "type" to "Resource not found"))
        is SonInfoIdNotFound -> respond(
            HttpStatusCode.NotFound,
            mapOf("error" to e.completeMessage, "type" to "Resource not found"))
        is AuthNotValidException -> respond(
            HttpStatusCode.Unauthorized,
            mapOf("error" to e.completeMessage, "type" to "Signup error"))
        is ServerException -> respond(
            HttpStatusCode(e.statusCode, e.localizedMessage),
            mapOf("error" to e.completeMessage))
        is NotFoundException -> respond(
            HttpStatusCode.NotFound,
            mapOf("error" to e.completeMessage)
        )
        is ForbiddenException -> respond(
            HttpStatusCode.Forbidden,
            mapOf("error" to e.completeMessage, "type" to "Forbidden error"))
        is UsernameAlreadyExists -> respond(
            HttpStatusCode.Conflict,
            mapOf("error" to e.completeMessage, "type" to "Conflict error"))
        is IllegalArgumentException -> {
            if (e.localizedMessage.contains("ObjectId")) respond( // Mongo ID not found
                HttpStatusCode.NotFound,
                mapOf("error" to e.completeMessage)
            )
            else respond(
                HttpStatusCode(400, "Input error"),
                mapOf("error" to e.completeMessage))
        }
        is UnsupportedMediaTypeException -> respond(
            HttpStatusCode(HttpStatusCode.BadRequest.value, "Bad request"),
            mapOf("error" to e.completeMessage))
        is IOException -> respond(
            HttpStatusCode(HttpStatusCode.BadRequest.value, "Bad request"),
            mapOf("error" to e.completeMessage))
        is EnumValueException -> respond(
            HttpStatusCode(HttpStatusCode.BadRequest.value, "Bad request"),
            mapOf("error" to e.text))
        is BadRequestException -> respond(
            HttpStatusCode(HttpStatusCode.BadRequest.value, e.localizedMessage),
            mapOf("error" to e.completeMessage))
        is TokenExpiredException -> respond(
            HttpStatusCode(HttpStatusCode.Unauthorized.value, "Not authorized"),
            mapOf("error" to e.completeMessage))
        is SignatureVerificationException -> respond(
            HttpStatusCode(HttpStatusCode.Unauthorized.value, "Invalid signature"),
            mapOf("error" to "Invalid signature"))
        is JsonParseException -> respond(
            HttpStatusCode(HttpStatusCode.BadRequest.value, "Bad request"),
            mapOf("error" to e.completeMessage))
        is MapCheckException -> respond(
            HttpStatusCode(HttpStatusCode.BadRequest.value, "Bad request"),
            mapOf("error" to e.completeMessage))
        is MongoWriteException -> respond(
            HttpStatusCode(HttpStatusCode.BadRequest.value, "Bad request"),
            mapOf("error" to e.completeMessage))
        is MissingPrivacyException -> respond(
            HttpStatusCode(423, e.localizedMessage),
            mapOf("error" to e.completeMessage))
        else -> respond(
            HttpStatusCode.InternalServerError,
            mapOf("error" to e.completeMessage, "type" to "Generic error", "exception" to e.toString()))
    }
}

val Exception.completeMessage get() = "${this.localizedMessage} ${this.cause}".trim()
