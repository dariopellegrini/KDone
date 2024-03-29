package com.dariopellegrini.kdone.privacy

import auth.mongoId
import auth.userAuth
import com.dariopellegrini.kdone.application.mongoRepository
import com.dariopellegrini.kdone.exceptions.BadRequestException
import com.dariopellegrini.kdone.exceptions.MissingPrivacyException
import com.dariopellegrini.kdone.exceptions.NotFoundException
import com.dariopellegrini.kdone.extensions.respondWithException
import com.dariopellegrini.kdone.privacy.model.*
import io.ktor.server.application.call 
import io.ktor.server.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import  io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import org.litote.kmongo.eq

fun Route.privacyModule(prefix: String? = null, paragraphs: List<PrivacyParagraph>) {

    val repository = mongoRepository<UserPrivacy>("users_privacy")

    val currentPrefix = prefix?.let { "$it/" } ?: ""

    authenticate("jwt") {
        get("${currentPrefix}privacy/content") {
            try {
                call.respond(HttpStatusCode.OK, paragraphs)
            } catch (e: Exception) {
                call.respondWithException(e)
            }
        }

        get("${currentPrefix}privacy/contents") {
            try {
                call.respond(HttpStatusCode.OK, paragraphs)
            } catch (e: Exception) {
                call.respondWithException(e)
            }
        }

        post("${currentPrefix}privacy/me") {
            try {
                val userId = call.userAuth.userId
                val input = call.receive<PrivacyPreferencesInput>()

                if (repository.count(UserPrivacy::userId eq userId.mongoId()) > 0)
                    throw BadRequestException("Privacy already sent for this user")

                val paragraphsKeys = paragraphs.map { it.key }
                val inputKeys = input.preferences.map { it.key }
                inputKeys.forEach {
                    if (!paragraphsKeys.contains(it)) throw BadRequestException("Key $it is not a paragraph key")
                }

                val userPreferences = mutableListOf<UserPrivacyPreference>()

                paragraphs.forEach { paragraph ->
                    val preferenceInput = input.preferences.firstOrNull { it.key == paragraph.key }
                    if (preferenceInput != null && paragraph.selectable) {
                        if (paragraph.mustBeAnswered && preferenceInput.accepted == null)
                            throw BadRequestException("Paragraph ${preferenceInput.key} must be answered")
                        if (paragraph.mustBeAccepted && preferenceInput.accepted != true)
                            throw BadRequestException("Paragraph ${preferenceInput.key} must be accepted")
                        userPreferences.add(
                                UserPrivacyPreference(preferenceInput.key, preferenceInput.accepted)
                        )
                    } else if (paragraph.selectable && paragraph.mustBeAnswered) {
                        throw BadRequestException("Paragraph ${paragraph.key} must be answered")
                    }
                }

                val userPrivacy = UserPrivacy(
                    userId = userId.mongoId(),
                    preferences = userPreferences
                )
                repository.insert(userPrivacy)
                call.respond(HttpStatusCode.Created, mapOf("status" to "success"))
            } catch (e: Exception) {
                call.respondWithException(e)
            }
        }

        get("${currentPrefix}privacy/me/filled") {
            try {
                val userId = call.userAuth.userId
                val count = repository.count(UserPrivacy::userId eq userId.mongoId())
                if (count == 0L) throw MissingPrivacyException()
                call.respond(HttpStatusCode.NoContent)
            } catch (e: Exception) {
                call.respondWithException(e)
            }
        }

        get("${currentPrefix}privacy/me") {
            try {
                val userId = call.userAuth.userId
                val userPrivacy = repository.findOneOrNull(UserPrivacy::userId eq userId.mongoId()) ?: throw NotFoundException("Privacy not found for this user")
                val reports = userPrivacy.preferences.map { preference ->
                    val paragraph = paragraphs.firstOrNull { it.key == preference.key }
                    UserPrivacyReport(preference, paragraph)
                }
                call.respond(HttpStatusCode.OK, reports)
            } catch (e: Exception) {
                call.respondWithException(e)
            }
        }

        patch("${currentPrefix}privacy/me") {
            try {
                val userId = call.userAuth.userId
                val input = call.receive<PrivacyPreferencesInput>()
                val userPrivacy = repository.findOneOrNull(UserPrivacy::userId eq userId.mongoId()) ?: throw NotFoundException("Privacy not found for this user")

                val userPreferences = userPrivacy.preferences.toMutableList()
                input.preferences.forEach { preferenceInput ->
                    val paragraph = paragraphs.firstOrNull { it.key == preferenceInput.key }
                    if (paragraph != null) {
                        if (paragraph.mustBeAnswered && preferenceInput.accepted == null)
                            throw BadRequestException("Paragraph ${preferenceInput.key} must be answered")
                        if (paragraph.mustBeAccepted && preferenceInput.accepted != true)
                            throw BadRequestException("Paragraph ${preferenceInput.key} must be accepted")

                        val index = userPreferences.indexOfFirst { it.key == preferenceInput.key }
                        if (index >= 0) {
                            userPreferences[index].accepted = preferenceInput.accepted
                        }
                    }
                }
                userPrivacy.preferences = userPreferences
                repository.updateOne(UserPrivacy::_id eq userPrivacy._id, userPrivacy)
                call.respond(HttpStatusCode.OK, mapOf("status" to "success"))
            } catch (e: Exception) {
                call.respondWithException(e)
            }
        }
    }
}