package com.dariopellegrini.kdone.user

import auth.mongoId
import auth.userAuth
import auth.userAuthOrNull
import com.dariopellegrini.kdone.application.database
import com.dariopellegrini.kdone.application.jwtConfiguration
import com.dariopellegrini.kdone.auth.*
import com.dariopellegrini.kdone.constants.queryParameter
import com.dariopellegrini.kdone.constants.usersTokensCollection
import com.dariopellegrini.kdone.exceptions.*
import com.dariopellegrini.kdone.extensions.*
import com.dariopellegrini.kdone.model.ResourceFile
import com.dariopellegrini.kdone.mongo.MongoRepository
import com.dariopellegrini.kdone.user.model.KDoneUser
import com.dariopellegrini.kdone.user.model.LoginInput
import com.dariopellegrini.kdone.user.model.UserToken
import com.dariopellegrini.kdone.user.social.apple.apple
import com.dariopellegrini.kdone.user.social.facebook.facebook
import com.dariopellegrini.kdone.user.social.google.google
import com.dariopellegrini.kdone.utils.HashUtils.sha512
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.request.isMultipart
import io.ktor.request.receive
import io.ktor.request.receiveMultipart
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.util.toMap
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.bson.conversions.Bson
import org.litote.kmongo.*
import java.util.*
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

inline fun <reified T : KDoneUser>Route.userModule(endpoint: String = "users",
                                                   configure: UserRouteConfiguration<T>.() -> Unit = {}) {

    val configuration = UserRouteConfiguration<T>()
    configuration.configure()

    val jwtConfig = jwtConfiguration

    val repository = MongoRepository(database, endpoint, T::class.java)

    // users_token should be reserved
    val tokenRepository = MongoRepository(database, usersTokensCollection, UserToken::class.java)

    T::class.java.geoIndexJson?.forEach {
        repository.createIndex(it)
    }

    authenticate("jwt", optional = true) {
        post(endpoint) {
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

                val input = if (call.request.isMultipart()) {
                    val uploader = configuration.uploader ?: throw ServerException(500, "Uploader not configured")
                    call.receiveMultipart().receive<T>(uploader) { map ->
                        val username = map["username"] ?: throw ServerException(400, "Missing username")
                        map["password"] ?: throw ServerException(400, "Missing password")
                        val role = map["role"]
                        checkInput(username, role)
                    }
                } else {
                    val input = call.receive<T>()
                    checkInput(input.username, input.role)
                    input
                }

                if (userAuth?.role != null) {
                    if (input.role != null) {
                        if (!configuration.authorization.check(userAuth.role, create, input.role!!)) throw NotAuthorizedException()
                    } else {
                        if (!configuration.authorization.check(userAuth.role, create, registered)) throw NotAuthorizedException()
                    }
                } else if (userAuth != null) {
                    if (input.role != null) {
                        if (!configuration.authorization.check(registered, create, input.role!!)) throw NotAuthorizedException()
                    } else {
                        if (!configuration.authorization.check(registered, create, registered)) throw NotAuthorizedException()
                    }
                } else {
                    if (input.role != null) {
                        if (!configuration.authorization.check(guest, create, input.role!!)) throw NotAuthorizedException()
                    } else {
                        if (!configuration.authorization.check(guest, create, registered)) throw NotAuthorizedException()
                    }
                }

                if (repository.count(KDoneUser::username eq input.username) > 0) throw UsernameAlreadyExists()

                val password = input.password ?: throw ServerException(400, "Missing password")
                input.password = if (configuration.hashStrategy != null) configuration.hashStrategy!!.invoke(password) else sha512(password)
                repository.insert(input)
                call.respond(input.secure())
            } catch (e: Exception) {
                call.respondWithException(e)
            }
        }

        get(endpoint) {
            try {
                val userAuth = call.userAuthOrNull
                if (userAuth != null) call.checkToken(this@authenticate.database)

                // Filters
                val queryMap = mutableMapOf<String, Any>()
                call.request.queryParameters.toMap().filter { it.key != queryParameter }.map { it.key to it.value.first() }.map { pair ->
                    when {
                        pair.second.toIntOrNull() != null -> queryMap[pair.first] = pair.second.toInt()
                        pair.second.toDoubleOrNull() != null -> queryMap[pair.first] = pair.second.toDouble()
                        pair.second == "true" -> queryMap[pair.first] = true
                        pair.second == "false" -> queryMap[pair.first] = false
                        else -> queryMap[pair.first] = pair.second
                    }
                }
                val mongoQuery = call.parameters[queryParameter]
                val query = if (mongoQuery != null && queryMap.isNotEmpty()) {
                    val first = queryMap.json.removeSuffix("}")
                    val second = mongoQuery.removePrefix("{").removeSuffix("}")
                    "$first, $second}"
                } else mongoQuery ?: queryMap.json

                val users = when {
                    userAuth?.role != null -> {
                        var bsonList: MutableList<Bson>? = null
                        if (configuration.authorization.check(userAuth.role, read, registered)) {
                            if (bsonList == null) bsonList = mutableListOf()
                            bsonList.add(KDoneUser::role eq null)
                        }
                        configuration.authorization.acceptableRoles(userAuth.role, read).forEach {
                            if (bsonList == null) bsonList = mutableListOf()
                            bsonList!!.add(KDoneUser::role eq it)
                        }
                        bsonList?.let { repository.findAll(and(or(it), query.bson)) } ?: run { listOf<T>() }
                    }
                    userAuth != null -> {
                        var bsonList: MutableList<Bson>? = null
                        if (configuration.authorization.check(registered, read, registered)) {
                            if (bsonList == null) bsonList = mutableListOf()
                            bsonList!!.add(KDoneUser::role eq null)
                        }
                        configuration.authorization.acceptableRoles(registered, read).forEach {
                            if (bsonList == null) bsonList = mutableListOf()
                            bsonList!!.add(KDoneUser::role eq it)
                        }
                        bsonList?.let { repository.findAll(and(or(it), query.bson)) } ?: run { listOf<T>() }
                    }
                    else -> {
                        var bsonList: MutableList<Bson>? = null
                        if (configuration.authorization.check(guest, read, registered)) {
                            if (bsonList == null) bsonList = mutableListOf()
                            bsonList!!.add(KDoneUser::role eq null)
                        }
                        configuration.authorization.acceptableRoles(guest, read).forEach {
                            if (bsonList == null) bsonList = mutableListOf()
                            bsonList!!.add(KDoneUser::role eq it)
                        }
                        bsonList?.let { repository.findAll(and(or(it), query.bson)) } ?: run { listOf<T>() }
                    }
                }

                call.respond(HttpStatusCode.OK, users.map { it.secure() })
            } catch (e: Exception) {
                call.respondWithException(e)
            }
        }

        get("$endpoint/{id}") {
            try {
                val userAuth = call.userAuthOrNull
                if (userAuth != null) call.checkToken(this@authenticate.database)

                val id = call.parameters["id"] ?: throw ServerException(400, "Missing id")

                val user = repository.findById(id.mongoId())

                // Owner before everything
                if (user._id.toString() == userAuth?.userId && configuration.authorization.checkOwner(read)) {
                    call.respond(HttpStatusCode.OK, user.secure())
                } else {
                    when {
                        // Role
                        userAuth?.role != null -> {
                            if (!configuration.authorization.check(userAuth.role, read, user.role ?: registered.rawValue)) throw ForbiddenException()
                        }
                        // Registered
                        userAuth != null -> {
                            if (!configuration.authorization.check(registered, read, user.role ?: registered.rawValue)) throw ForbiddenException()
                        }
                        // Guest
                        else -> {
                            if (!configuration.authorization.check(guest, read, user.role ?: registered.rawValue)) throw ForbiddenException()
                        }
                    }
                    call.respond(HttpStatusCode.OK, user.secure())
                }
            } catch (e: Exception) {
                call.respondWithException(e)
            }
        }

        delete("$endpoint/{id}") {
            try {
                val userAuth = call.userAuthOrNull
                if (userAuth != null) call.checkToken(this@authenticate.database)

                val id = call.parameters["id"] ?: throw ServerException(400, "Missing id")

                val user = repository.findById(id.mongoId())

                // Owner before everything
                if (user._id.toString() == userAuth?.userId && configuration.authorization.checkOwner(delete)) {
                    call.respond(HttpStatusCode.OK, user.secure())
                } else {
                    when {
                        // Role
                        userAuth?.role != null -> {
                            if (!configuration.authorization.check(userAuth.role, delete, user.role ?: registered.rawValue)) throw ForbiddenException()
                        }
                        // Registered
                        userAuth != null -> {
                            if (!configuration.authorization.check(registered, delete, user.role ?: registered.rawValue)) throw ForbiddenException()
                        }
                        // Guest
                        else -> {
                            if (!configuration.authorization.check(guest, delete, user.role ?: registered.rawValue)) throw ForbiddenException()
                        }
                    }

                    // Deleting files
                    val urls: List<String>? = if (configuration.uploader != null) {
                        val resources = T::class.declaredMemberProperties.filter { it.returnType.jvmErasure.isSubclassOf(
                            ResourceFile::class) }
                        val element = repository.findById(id.mongoId())

                        resources.map { it.get(element) }.filterIsInstance(ResourceFile::class.java).map { it.url }
                    } else {
                        null
                    }

                    val result = repository.deleteById(id.mongoId())
                    tokenRepository.deleteMany(UserToken::userId eq id.mongoId())

                    configuration.uploader?.let {
                            uploader ->
                        val jobs = urls?.map { url ->
                            async { uploader.delete(url) }
                        }
                        jobs?.awaitAll()
                    }

                    call.respond(HttpStatusCode.OK, result)
                }
            } catch (e: Exception) {
                call.respondWithException(e)
            }
        }

        patch("$endpoint/{id}") {
            try {
                val userAuth = call.userAuthOrNull
                if (userAuth != null) call.checkToken(this@authenticate.database)

                val id = call.parameters["id"] ?: throw ServerException(400, "Missing id")
                val user = repository.findById(id.mongoId())

                val checkInput: suspend (String?, String?) -> Unit = {
                        username, role ->

                    when {
                        // Role
                        userAuth?.role != null -> {
                            if (!configuration.authorization.check(userAuth.role, update, user.role ?: registered.rawValue)) throw ForbiddenException()
                            role?.let {
                                if (!configuration.authorization.check(userAuth.role, create, it)) throw ForbiddenException()
                            }
                        }
                        // Registered
                        userAuth != null -> {
                            if (!configuration.authorization.check(registered, update, user.role ?: registered.rawValue)) throw ForbiddenException()
                            role?.let {
                                if (!configuration.authorization.check(registered, create, it)) throw ForbiddenException()
                            }
                        }
                        // Guest
                        else -> {
                            if (!configuration.authorization.check(guest, delete, user.role ?: registered.rawValue)) throw ForbiddenException()
                            role?.let {
                                if (!configuration.authorization.check(guest, create, it)) throw ForbiddenException()
                            }
                        }
                    }

                    username?.let {
                        if (repository.count(KDoneUser::username eq it) > 0) throw UsernameAlreadyExists()
                    }
                }

                if (call.request.isMultipart()) {
                    val uploader = configuration.uploader ?: throw ServerException(500, "Uploader not configured")
                    val patch = call.receiveMultipartMap<T>(uploader) {
                        if (it.containsKey("password")) throw ForbiddenException("Cannot change password")
                        checkInput(it["username"] as? String, it["role"] as? String)
                    }
                    repository.updateOneById(id.mongoId(), patch)
                    call.respond(HttpStatusCode.OK, repository.findById(id.mongoId()).secure())
                } else {
                    val patch = call.receiveMap<T>()
                    if (patch.containsKey("password")) throw ForbiddenException("Cannot change password")
                    checkInput(patch["username"] as? String, patch["role"] as? String)
                    repository.updateOneById(id.mongoId(), patch)
                    call.respond(HttpStatusCode.OK, repository.findById(id.mongoId()).secure())
                }
            } catch (e: Exception) {
                call.respondWithException(e)
            }
        }
    }

    post("$endpoint/auth/login") {
        try {
            val input = call.receive<LoginInput>()
            val password = if (configuration.hashStrategy != null)
                configuration.hashStrategy!!.invoke(input.password)
            else sha512(input.password)
            val user = repository.findOneOrNull(
                KDoneUser::username eq input.username,
                KDoneUser::password eq password
            ) ?: throw NotAuthorizedException()
            val token = jwtConfig.makeToken(UserAuth(user._id.toString(), user.role))
            call.response.header(HttpHeaders.Authorization, token)
            tokenRepository.insert(UserToken(user._id, token, Date()))

            call.respond(HttpStatusCode.OK, user.secure())
        } catch (e: Exception) {
            call.respondWithException(e)
        }
    }

    authenticate("jwt") {
        get("$endpoint/profile/me") {
            try {
                if (!configuration.authorization.checkOwner(read)) throw NotAuthorizedException()
                call.checkToken(this@authenticate.database)
                val user = repository.findById(call.userAuth.userId.mongoId())
                call.respond(HttpStatusCode.OK, user.secure())
            } catch (e: Exception) {
                call.respondWithException(e)
            }
        }

        delete("$endpoint/profile/me") {
            try {
                if (!configuration.authorization.checkOwner(delete)) throw NotAuthorizedException()
                call.checkToken(this@authenticate.database)
                val result = repository.deleteById(call.userAuth.userId.mongoId())
                tokenRepository.deleteMany(UserToken::userId eq call.userAuth.userId.mongoId())
                call.respond(HttpStatusCode.OK, result)
            } catch (e: Exception) {
                call.respondWithException(e)
            }
        }

        patch("$endpoint/profile/me/") {
            try {
                val userAuth = call.userAuth

                if (!configuration.authorization.checkOwner(update)) throw NotAuthorizedException()
                call.checkToken(this@authenticate.database)

                val patch: Map<String, Any> = if (call.request.isMultipart()) {
                    val uploader = configuration.uploader ?: throw ServerException(500, "Uploader not configured")
                    call.receiveMultipartMap<T>(uploader)
                } else {
                    call.receiveMap<T>()
                }

                (patch["role"] as? String)?.let {
                    newRole ->

                    when {
                        // Role
                        userAuth.role != null -> {
                            if (!configuration.authorization.check(userAuth.role, create, newRole)) throw ForbiddenException()
                        }
                        // Registered
                        else -> {
                            if (!configuration.authorization.check(registered, create, newRole)) throw ForbiddenException()
                        }
                    }
                }

                call.respond(HttpStatusCode.OK, repository.updateOneById(call.userAuth.userId.mongoId(), patch))
            } catch (e: Exception) {
                call.respondWithException(e)
            }
        }

        post("$endpoint/auth/logout") {
            try {
                val userAuth = call.userAuth
                val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")
                call.checkToken(this@authenticate.database)

                val result = if (call.request.queryParameters["all"] == "true") {
                    tokenRepository.deleteMany(UserToken::userId eq userAuth.userId.mongoId())
                } else {
                    tokenRepository.deleteMany(UserToken::token eq token)
                }
                call.respond(HttpStatusCode.OK, result)
            } catch (e: Exception) {
                call.respondWithException(e)
            }
        }
    }

    // Social

    configuration.facebook?.let {
        facebook(it.appId, it.appSecret, repository, tokenRepository, jwtConfig, configuration)
    }

    configuration.apple?.let {
        apple(it.bundleId, repository, tokenRepository, jwtConfig, configuration)
    }

    configuration.google?.let {
        google(it.clientId, it.clientSecret, it.redirectURL, repository, tokenRepository, jwtConfig, configuration)
    }
}

