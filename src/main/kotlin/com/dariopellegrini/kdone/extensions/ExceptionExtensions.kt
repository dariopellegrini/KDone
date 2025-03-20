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
    
    when (e) {
        is UserIdNotVerifiedForDocument -> logAndRespond(
            HttpStatusCode.Unauthorized,
            mapOf("error" to e.completeMessage, "type" to "Authorization error"))
        is NotAuthorizedException -> logAndRespond(
            HttpStatusCode.Unauthorized,
            mapOf("error" to e.completeMessage, "type" to "Authorization error"))
        is SignUpErrorException -> logAndRespond(
            HttpStatusCode.BadRequest,
            mapOf("error" to e.completeMessage, "type" to "Authorization error"))
        is MissingDocumentId -> logAndRespond(
            HttpStatusCode.Unauthorized,
            mapOf("error" to e.completeMessage, "type" to "Request error"))
        is MismatchedInputException -> logAndRespond(
            HttpStatusCode.BadRequest,
            mapOf("error" to e.completeMessage, "type" to "Input error"))
        is JsonMappingException -> logAndRespond(
            HttpStatusCode.BadRequest,
            mapOf("error" to e.completeMessage, "type" to "Input error"))
        is MissingAccessToken -> logAndRespond(
            HttpStatusCode.BadRequest,
            mapOf("error" to e.completeMessage, "type" to "Request error"))
        is MissingRefreshToken -> logAndRespond(
            HttpStatusCode.BadRequest,
            mapOf("error" to e.completeMessage, "type" to "Request error"))
        is MissingUsername -> logAndRespond(
            HttpStatusCode.BadRequest,
            mapOf("error" to e.completeMessage, "type" to "Request error"))
        is MissingPassword -> logAndRespond(
            HttpStatusCode.BadRequest,
            mapOf("error" to e.completeMessage, "type" to "Request error"))
        is MissingMotherInfoId -> logAndRespond(
            HttpStatusCode.BadRequest,
            mapOf("error" to e.completeMessage, "type" to "Request error"))
        is MissingSonInfoId -> logAndRespond(
            HttpStatusCode.BadRequest,
            mapOf("error" to e.completeMessage, "type" to "Request error"))
        is DocumentNotFound -> logAndRespond(
            HttpStatusCode.NotFound,
            mapOf("error" to e.completeMessage, "type" to "Resource not found"))
        is MotherInfoIdNotFound -> logAndRespond(
            HttpStatusCode.NotFound,
            mapOf("error" to e.completeMessage, "type" to "Resource not found"))
        is SonInfoIdNotFound -> logAndRespond(
            HttpStatusCode.NotFound,
            mapOf("error" to e.completeMessage, "type" to "Resource not found"))
        is AuthNotValidException -> logAndRespond(
            HttpStatusCode.Unauthorized,
            mapOf("error" to e.completeMessage, "type" to "Signup error"))
        is ServerException -> logAndRespond(
            HttpStatusCode(e.statusCode, e.localizedMessage),
            mapOf("error" to e.completeMessage))
        is NotFoundException -> logAndRespond(
            HttpStatusCode.NotFound,
            mapOf("error" to e.completeMessage)
        )
        is ForbiddenException -> logAndRespond(
            HttpStatusCode.Forbidden,
            mapOf("error" to e.completeMessage, "type" to "Forbidden error"))
        is UsernameAlreadyExists -> logAndRespond(
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
        is UnsupportedMediaTypeException -> logAndRespond(
            HttpStatusCode(HttpStatusCode.BadRequest.value, "Bad request"),
            mapOf("error" to e.completeMessage))
        is IOException -> logAndRespond(
            HttpStatusCode(HttpStatusCode.BadRequest.value, "Bad request"),
            mapOf("error" to e.completeMessage))
        is EnumValueException -> logAndRespond(
            HttpStatusCode(HttpStatusCode.BadRequest.value, "Bad request"),
            mapOf("error" to e.text))
        is BadRequestException -> logAndRespond(
            HttpStatusCode(HttpStatusCode.BadRequest.value, e.localizedMessage),
            mapOf("error" to e.completeMessage))
        is TokenExpiredException -> logAndRespond(
            HttpStatusCode(HttpStatusCode.Unauthorized.value, "Not authorized"),
            mapOf("error" to e.completeMessage))
        is SignatureVerificationException -> logAndRespond(
            HttpStatusCode(HttpStatusCode.Unauthorized.value, "Invalid signature"),
            mapOf("error" to "Invalid signature"))
        is JsonParseException -> logAndRespond(
            HttpStatusCode(HttpStatusCode.BadRequest.value, "Bad request"),
            mapOf("error" to e.completeMessage))
        is MapCheckException -> logAndRespond(
            HttpStatusCode(HttpStatusCode.BadRequest.value, "Bad request"),
            mapOf("error" to e.completeMessage))
        is MongoWriteException -> logAndRespond(
            HttpStatusCode(HttpStatusCode.BadRequest.value, "Bad request"),
            mapOf("error" to e.completeMessage))
        is MissingPrivacyException -> logAndRespond(
            HttpStatusCode(423, e.localizedMessage),
            mapOf("error" to e.completeMessage))
        else -> logAndRespond(
            HttpStatusCode.InternalServerError,
            mapOf("error" to e.completeMessage, "type" to "Generic error", "exception" to e.toString()))
    }
}

val Exception.completeMessage get() = "${this.localizedMessage} ${this.cause}".trim()

suspend fun ApplicationCall.logAndRespond(statusCode: HttpStatusCode, responseJson: Map<String, String>) {
    application.environment.log.error("${statusCode.value} -> $responseJson")
    respond(statusCode, responseJson)
}
