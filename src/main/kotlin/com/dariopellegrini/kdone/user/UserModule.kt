package com.dariopellegrini.kdone.user

import auth.mongoId
import auth.userAuth
import auth.userAuthOrNull
import com.dariopellegrini.kdone.application.database
import com.dariopellegrini.kdone.application.jwtConfiguration
import com.dariopellegrini.kdone.auth.*
import com.dariopellegrini.kdone.constants.passwordsRecoveryCollection
import com.dariopellegrini.kdone.constants.queryParameter
import com.dariopellegrini.kdone.constants.usersConfirmationsCollection
import com.dariopellegrini.kdone.constants.usersTokensCollection
import com.dariopellegrini.kdone.email.EmailConfirmationConfiguration
import com.dariopellegrini.kdone.email.model.UserConfirmation
import com.dariopellegrini.kdone.exceptions.*
import com.dariopellegrini.kdone.extensions.*
import com.dariopellegrini.kdone.model.ResourceFile
import com.dariopellegrini.kdone.model.SoftDeletable
import com.dariopellegrini.kdone.mongo.MongoRepository
import com.dariopellegrini.kdone.passwordrecovery.model.PasswordRecovery
import com.dariopellegrini.kdone.privacy.privacyModule
import com.dariopellegrini.kdone.user.model.*
import com.dariopellegrini.kdone.user.otp.otpModule
import com.dariopellegrini.kdone.user.social.apple.apple
import com.dariopellegrini.kdone.user.social.facebook.facebook
import com.dariopellegrini.kdone.user.social.google.google
import com.dariopellegrini.kdone.utils.HashUtils.sha512
import com.dariopellegrini.kdone.utils.randomString
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.request.isMultipart
import io.ktor.request.receive
import io.ktor.request.receiveMultipart
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.*
import io.ktor.util.error
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
                                                   collectionName: String? = null,
                                                   configure: UserRouteConfiguration<T>.() -> Unit = {}) {

    val configuration = UserRouteConfiguration<T>()
    configuration.configure()

    val jwtConfig = jwtConfiguration

    val repository = MongoRepository(database, collectionName ?: endpoint, T::class.java)

    configuration.repository = repository

    // users_token should be reserved
    val tokenRepository = MongoRepository(database, usersTokensCollection, UserToken::class.java)
    val emailConfirmationRepository = MongoRepository(database, usersConfirmationsCollection, UserConfirmation::class.java)
    val passwordRecoveryRepository = MongoRepository(database, passwordsRecoveryCollection, PasswordRecovery::class.java)

    T::class.java.geoIndexJson?.forEach {
        repository.createIndex(it)
    }

    val emailConfirmationConfiguration: EmailConfirmationConfiguration? = configuration.emailConfirmationConfiguration

    if (configuration.needsEmailConfirmation == true && configuration.emailConfirmationConfiguration == null) {
        throw ServerException(500, "E-mail confirmation not configured")
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

                configuration.beforeCreate?.let {
                    it(call, input)
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
                input.password = if (configuration.hashStrategy != null) configuration.hashStrategy!!.hash(password) else sha512(password)

                if (configuration.loggedInAfterSignUp == true && configuration.needsEmailConfirmation != true) {
                    val token = jwtConfig.makeToken(UserAuth(input._id.toString(), input.role))
                    call.response.header(HttpHeaders.Authorization, token)
                    tokenRepository.insert(UserToken(input._id, token, Date()))
                }

                repository.insert(input)
                call.respond(input.secure())

                configuration.afterCreate?.let { it(call, input) }

                if (emailConfirmationConfiguration != null) {
                    val code = randomString(32)
                    val userConfirmation = UserConfirmation(input._id, input.username, code, false)
                    emailConfirmationRepository.insert(userConfirmation)
                    val link = "${emailConfirmationConfiguration.baseURL}/$endpoint/auth/verify/$code".normalizeURL
                    val message = emailConfirmationConfiguration.emailSenderClosure(link)
                    try {
                        emailConfirmationConfiguration.emailClient.send(
                            message.sender.name to message.sender.address,
                            input.username to input.username,
                            message.subject, message.message)
                        logger.info("E-mail sent for ${input.username}")
                    } catch (e: Exception) {
                        logger.error(e)
                    }
                }
            } catch (e: Exception) {
                call.respondWithException(e)
                configuration.exceptionHandler?.invoke(call, e)
            }
        }

        get(endpoint) {
            try {
                val userAuth = call.userAuthOrNull
                if (userAuth != null) call.checkToken(this@authenticate.database)

                // Filters
                val queryMap = mutableMapOf<String, Any>()
                call.request.queryParameters.toMap()
//                    .filter { it.key != queryParameter }
                    .map { it.key to it.value.first() }.map { pair ->
                    when {
                        pair.second.toIntOrNull() != null -> queryMap[pair.first] = pair.second.toInt()
                        pair.second.toDoubleOrNull() != null -> queryMap[pair.first] = pair.second.toDouble()
                        pair.second == "true" -> queryMap[pair.first] = true
                        pair.second == "false" -> queryMap[pair.first] = false
                        else -> queryMap[pair.first] = pair.second
                    }
                }

                configuration.beforeGet?.let { it(call, queryMap) }

                val mongoQuery = queryMap[queryParameter] as? String
                queryMap.remove(queryParameter)

//                val mongoQuery = call.parameters[queryParameter]
                val query = if (mongoQuery != null && queryMap.isNotEmpty()) {
                    val first = queryMap.json.removeSuffix("}")
                    val second = mongoQuery.removePrefix("{").removeSuffix("}")
                    "$first, $second}"
                } else mongoQuery ?: queryMap.json

//                if (!queryMap.contains("softDeleted") &&
//                    configuration.enableSoftDelete &&
//                    T::class.isSubclassOf(SoftDeletable::class)) {
//                    queryMap["softDeleted"] = false
//                }

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

                call.respond(HttpStatusCode.OK,
                    if (configuration.useObjectsForArrays) {
                        mapOf("results" to users.map { it.secure() })
                    } else {
                        users.map { it.secure() }
                    }
                )
            } catch (e: Exception) {
                call.respondWithException(e)
                configuration.exceptionHandler?.invoke(call, e)
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
                configuration.exceptionHandler?.invoke(call, e)
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
                configuration.exceptionHandler?.invoke(call, e)
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
                            if (!configuration.authorization.check(guest, update, user.role ?: registered.rawValue)) throw ForbiddenException()
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

                    configuration.beforeUpdate?.invoke(call, id.mongoId(), patch.toMutableMap())

                    repository.updateOneById(id.mongoId(), patch)
                    call.respond(HttpStatusCode.OK, repository.findById(id.mongoId()).secure())
                    configuration.afterUpdate?.let {
                        it(call, patch, user)
                    }
                } else {
                    val patch = call.receiveMap<T>()
                    if (patch.containsKey("password")) throw ForbiddenException("Cannot change password")
                    checkInput(patch["username"] as? String, patch["role"] as? String)

                    configuration.beforeUpdate?.invoke(call, id.mongoId(), patch.toMutableMap())

                    repository.updateOneById(id.mongoId(), patch)
                    call.respond(HttpStatusCode.OK, repository.findById(id.mongoId()).secure())
                    configuration.afterUpdate?.let {
                        it(call, patch, user)
                    }
                }
            } catch (e: Exception) {
                call.respondWithException(e)
                configuration.exceptionHandler?.invoke(call, e)
            }
        }
    }

    post("$endpoint/auth/login") {
        try {
            if (configuration.loginDisabled) throw ForbiddenException("Forbidden operation")

            val input = call.receive<LoginInput>()

            val user = if (configuration.hashStrategy != null) {
                val user = repository.findOneOrNull(
                    KDoneUser::username eq input.username) ?: throw NotAuthorizedException()
                val password = user.password ?: throw NotAuthorizedException()
                if (!configuration.hashStrategy!!.verify(input.password, password)) throw NotAuthorizedException()
                user
            } else {
                val password = sha512(input.password)
                repository.findOneOrNull(
                    KDoneUser::username eq input.username,
                    KDoneUser::password eq password
                ) ?: throw NotAuthorizedException()
            }

            if (configuration.needsEmailConfirmation == true && user.confirmed != true) {
                throw ForbiddenException("This account has not been confirmed")
            }

            val token = jwtConfig.makeToken(UserAuth(user._id.toString(), user.role))
            call.response.header(HttpHeaders.Authorization, token)
            tokenRepository.insert(UserToken(user._id, token, Date()))

            call.respond(HttpStatusCode.OK, user.secure())
        } catch (e: Exception) {
            call.respondWithException(e)
            configuration.exceptionHandler?.invoke(call, e)
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
                configuration.exceptionHandler?.invoke(call, e)
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
                configuration.exceptionHandler?.invoke(call, e)
            }
        }

        patch("$endpoint/profile/me") {
            try {
                val userAuth = call.userAuth

                if (!configuration.authorization.checkOwner(update)) throw NotAuthorizedException()
                call.checkToken(this@authenticate.database)

                val patch: Map<String, Any?> = if (call.request.isMultipart()) {
                    val uploader = configuration.uploader ?: throw ServerException(500, "Uploader not configured")
                    call.receiveMultipartMap<T>(uploader) { map ->
                        ownerForbiddenAttributes.forEach {
                            if (map.containsKey(it)) throw ForbiddenException("Cannot change $it")
                        }
                    }
                } else {
                    val map = call.receiveMap<T>()
                    ownerForbiddenAttributes.forEach {
                        if (map.containsKey(it)) throw ForbiddenException("Cannot change $it")
                    }
                    map
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
                configuration.exceptionHandler?.invoke(call, e)
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
                configuration.exceptionHandler?.invoke(call, e)
            }
        }

        // Password
        patch("$endpoint/profile/me/password") {
            try {
                val userAuth = call.userAuth
                if (!configuration.authorization.checkOwner(update)) throw NotAuthorizedException()
                call.checkToken(this@authenticate.database)

                val user = repository.findOneOrNull(KDoneUser::_id eq userAuth.userId.mongoId()) ?: throw NotAuthorizedException()
                if (user.password == null) throw BadRequestException("Impossible to change password for this user: password not found")

                val input = call.receive<PasswordChangeInput>()

                val currentHashedPassword = if (configuration.hashStrategy != null)
                    configuration.hashStrategy!!.hash(input.currentPassword) else sha512(input.currentPassword)

                if (user.password != currentHashedPassword) throw ForbiddenException("Incorrect password")

                val newPasswordHashed = if (configuration.hashStrategy != null)
                    configuration.hashStrategy!!.hash(input.newPassword) else sha512(input.newPassword)

                repository.updateOneById(userAuth.userId.mongoId(), mapOf("password" to newPasswordHashed))

                if (input.invalidateOtherSessions == true) {
                    val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")
                    tokenRepository.deleteMany(UserToken::token ne token)
                }

                call.respond(HttpStatusCode.OK, mapOf("status" to "Password changed successful"))
            } catch (e: Exception) {
                call.respondWithException(e)
            }
        }
    }

    get("$endpoint/auth/verify/{code}") {
        try {
            val code = call.parameters["code"] ?: throw BadRequestException("Missing code")
            val userConfirmation = emailConfirmationRepository.findOne(UserConfirmation::code eq code)
            if (userConfirmation.confirmed) throw ServerException(400, "E-mail already confirmed")

            repository.updateOne(KDoneUser::_id eq userConfirmation.userId, mapOf("confirmed" to true))

            emailConfirmationRepository.updateOne(UserConfirmation::_id eq userConfirmation._id, mapOf("confirmed" to true, "dateUpdated" to Date()))
            if (emailConfirmationConfiguration?.redirectURL != null) {
                call.respondRedirect(emailConfirmationConfiguration.redirectURL, true)
            } else {
                call.respond(HttpStatusCode.OK, mapOf("status" to "Confirmed"))
            }
        } catch (e: Exception) {
            call.respondWithException(e)
        }
    }

    // Password recovery
    post("$endpoint/password/recovery") {
        try {
            val passwordRecoveryConfiguration = configuration.passwordRecoveryConfiguration
                ?: throw ServerException(HttpStatusCode.NotImplemented.value, "Not configured")
            val input = call.receive<PasswordRecoveryInput>()
            val user = repository.findOne(KDoneUser::username eq input.email)

            if (user.password == null) throw ForbiddenException()

            val newPasswordHashed = if (configuration.hashStrategy != null)
                configuration.hashStrategy!!.hash(input.newPassword) else sha512(input.newPassword)

            passwordRecoveryRepository.deleteMany(PasswordRecovery::userId eq user._id)
            val passwordRecovery = PasswordRecovery(user._id, user.username, randomString(32), newPasswordHashed)
            passwordRecoveryRepository.insert(passwordRecovery)

            val link = "${passwordRecoveryConfiguration.baseURL}/$endpoint/password/recovery/verify/${passwordRecovery.code}".normalizeURL
            val message = passwordRecoveryConfiguration.emailSenderClosure(link)
            passwordRecoveryConfiguration.emailSender.send(
                message.sender.name to message.sender.address,
                input.email to input.email,
                message.subject, message.message)
            logger.info("Password recovery e-mail sent for ${input.email}")
            call.respond(HttpStatusCode.OK, mapOf("status" to "Password recovery e-mail sent", "email" to input.email))
        } catch (e: Exception) {
            call.respondWithException(e)
        }
    }

    get("$endpoint/password/recovery/verify/{code}") {
        try {
            val code = call.parameters["code"] ?: throw BadRequestException("Missing code")
            val passwordRecovery = passwordRecoveryRepository.findOneOrNull(
                PasswordRecovery::code eq code,
                PasswordRecovery::active eq true) ?: throw ForbiddenException("Already confirmed")
            repository.updateOne(KDoneUser::_id eq passwordRecovery.userId, mapOf("password" to passwordRecovery.newPassword))
            passwordRecoveryRepository.updateOne(PasswordRecovery::code eq code,
                mapOf("active" to false, "dateUpdated" to Date()))
            if (emailConfirmationConfiguration?.redirectURL != null) {
                call.respondRedirect(emailConfirmationConfiguration.redirectURL, true)
            } else {
                call.respond(HttpStatusCode.OK, mapOf("result" to "Password changed successful"))
            }
        } catch (e: Exception) {
            call.respondWithException(e)
        }
    }

    // Social

    configuration.facebook?.let {
        facebook(endpoint, it.appId, it.appSecret, repository, tokenRepository, jwtConfig, configuration)
    }

    configuration.apple?.let {
        apple(endpoint, it.bundleId, repository, tokenRepository, jwtConfig, configuration)
    }

    configuration.google?.let {
        google(endpoint, it.clientId, it.clientSecret, it.redirectURL, repository, tokenRepository, jwtConfig, configuration)
    }

    // OTP
    if (configuration.otpEnabled) {
        otpModule(endpoint, repository, tokenRepository, jwtConfig, configuration)
    }

    // Privacy
    configuration.privacyParagraphs?.let {
        privacyModule(it)
    }
}

