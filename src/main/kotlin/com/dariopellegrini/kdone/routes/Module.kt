package com.dariopellegrini.kdone.routes

import auth.mongoId
import auth.userAuth
import auth.userAuthOrNull
import com.dariopellegrini.kdone.application.database
import com.dariopellegrini.kdone.auth.AuthEnum
import com.dariopellegrini.kdone.auth.checkPermission
import com.dariopellegrini.kdone.auth.checkToken
import com.dariopellegrini.kdone.constants.queryParameter
import com.dariopellegrini.kdone.exceptions.BadRequestException
import com.dariopellegrini.kdone.exceptions.ForbiddenException
import com.dariopellegrini.kdone.exceptions.ServerException
import com.dariopellegrini.kdone.extensions.*
import com.dariopellegrini.kdone.model.Identifiable
import com.dariopellegrini.kdone.model.ResourceFile
import com.dariopellegrini.kdone.mongo.MongoRepository
import com.dariopellegrini.kdone.uploader.LocalUploader
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.request.isMultipart
import io.ktor.request.receive
import io.ktor.request.receiveMultipart
import io.ktor.response.respond
import io.ktor.response.respondBytes
import io.ktor.response.respondFile
import io.ktor.routing.*
import io.ktor.util.toMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.litote.kmongo.and
import org.litote.kmongo.eq
import org.litote.kmongo.json
import org.litote.kmongo.util.KMongoUtil
import java.io.File
import java.nio.file.Files
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.jvmErasure

inline fun <reified T : Any>Route.module(endpoint: String,
                                              configure: RouteConfiguration<T>.() -> Unit = {}) {

    val configuration = RouteConfiguration<T>()
    configuration.configure()
    val repository = MongoRepository(database, endpoint, T::class.java)

    T::class.java.geoIndexJson?.forEach {
        repository.createIndex(it)
    }

//    (configuration.uploader as? LocalUploader)?.let { localUploader ->
//        get("${localUploader.filesFolder}/{folder}/{fileName}") {
//            try {
//                val folder = call.parameters["folder"] ?: throw BadRequestException("Missing folder")
//                val fileName = call.parameters["fileName"] ?: throw BadRequestException("Missing folder")
//                val file = File("${localUploader.filesFolder}/$folder/$fileName")
////                val contentType = withContext(Dispatchers.IO) { Files.probeContentType(file.toPath()) }
//                call.respondFile(file)
//            } catch (e: Exception) {
//                call.respondWithException(e)
//            }
//        }
//    }

    authenticate("jwt", optional = true) {

        get(endpoint) {
            try {
                call.checkToken(this@authenticate.database)
                val shouldCheckOwner = checkPermission(call.userAuthOrNull, configuration.authorization, AuthEnum.READ)

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

                configuration.beforeGet?.let { it(call,
                    call.request.queryParameters.toMap().map { it.key to it.value.first() }.toMap())
                }

                val elements = if (shouldCheckOwner) repository.findAll(and(
                    Identifiable::owner eq call.userAuth.userId.mongoId(),
                    KMongoUtil.toBson(query)))
                else repository.findAll(query)

                call.respond(HttpStatusCode.OK, elements)

                configuration.afterGet?.let {
                    it(call,
                        call.request.queryParameters.toMap().map { it.key to it.value.first() }.toMap(),
                        elements)
                }
            } catch (e: Exception) {
                call.respondWithException(e)
            }
        }

        get("$endpoint/{id}") {
            try {
                call.checkToken(this@authenticate.database)
                val shouldCheckOwner = checkPermission(call.userAuthOrNull, configuration.authorization, AuthEnum.READ)

                val id = call.parameters["id"] ?: throw BadRequestException("Missing id")

                val element = repository.findById(id.mongoId())
                if (shouldCheckOwner) {
                    (element as? Identifiable)?.let {
                        if (it.owner.toString() != call.userAuth.userId) throw ForbiddenException()
                    }
                }

                call.respond(HttpStatusCode.OK, element)

                configuration.afterGet?.let {
                    it(call,
                        call.request.queryParameters.toMap().map { it.key to it.value.first() }.toMap(),
                        listOf(element))
                }
            } catch (e: Exception) {
                call.respondWithException(e)
            }
        }

        post(endpoint) {
            try {
                call.checkToken(this@authenticate.database)
                checkPermission(call.userAuthOrNull, configuration.authorization, AuthEnum.CREATE)

                val element = if (call.request.isMultipart()) {
                    val uploader = configuration.uploader ?: throw ServerException(500, "Uploader not configured")
                    call.receiveMultipart().receive(uploader)
                } else {
                    call.receive<T>()
                }

                configuration.beforeCreate?.let {
                    it(call, element)
                }

                (element as? Identifiable)?.owner = call.userAuthOrNull?.userId?.mongoId()

                repository.insert(element)

                call.respond(HttpStatusCode.OK, element)

                configuration.afterCreate?.let { it(call, element) }
            } catch (e: Exception) {
                call.respondWithException(e)
            }
        }

        patch("$endpoint/{id}") {
            try {
                call.checkToken(this@authenticate.database)
                val shouldCheckOwner = checkPermission(call.userAuthOrNull, configuration.authorization, AuthEnum.UPDATE)

                val id = call.parameters["id"] ?: throw ServerException(400, "Missing id")
                val patch = if (call.request.isMultipart()) {
                    val uploader = configuration.uploader ?: throw ServerException(500, "Uploader not configured")
                    call.receiveMultipartMap<T>(uploader)
                } else {
                    call.receiveMap<T>()
                }

                if (shouldCheckOwner) {
                    val element = repository.findById(id.mongoId())
                    (element as? Identifiable)?.owner?.let {
                        if (it.toString() != call.userAuth.userId) throw ForbiddenException()
                    } ?: run { throw ForbiddenException("No owner found") }
                }

                configuration.beforeUpdate?.let {
                    it(call, id.mongoId(), patch)
                }

                repository.updateOneById(id.mongoId(), patch)

                val updatedElement = repository.findById(id.mongoId())

                call.respond(HttpStatusCode.OK, updatedElement)

                configuration.afterUpdate?.let {
                    it(call, patch, updatedElement)
                }
            } catch (e: Exception) {
                call.respondWithException(e)
            }
        }

        delete(("$endpoint/{id}")) {
            try {
                call.checkToken(this@authenticate.database)
                val shouldCheckOwner = checkPermission(call.userAuthOrNull, configuration.authorization, AuthEnum.UPDATE)

                val id = call.parameters["id"] ?: throw ServerException(400, "Missing id")

                if (shouldCheckOwner) {
                    val element = repository.findById(id.mongoId())
                    (element as? Identifiable)?.owner?.let {
                        if (it.toString() != call.userAuth.userId) throw ForbiddenException()
                    } ?: run { throw ForbiddenException("No owner found") }
                }

                configuration.beforeDelete?.let {
                    it(call, id.mongoId())
                }

                // Deleting files
                val urls: List<String>? = if (configuration.uploader != null) {
                    val resources = T::class.declaredMemberProperties.filter { it.returnType.jvmErasure.isSubclassOf(ResourceFile::class) }
                    val element = repository.findById(id.mongoId())

                    resources.map { it.get(element) }.filterIsInstance(ResourceFile::class.java).map { it.url }
                } else {
                    null
                }

                val deleteResult = repository.deleteById(id.mongoId())

                configuration.uploader?.let {
                    uploader ->
                    val jobs = urls?.map { url ->
                        async { uploader.delete(url) }
                    }
                    jobs?.awaitAll()
                }

                call.respond(HttpStatusCode.OK, deleteResult)

                configuration.afterDelete?.let {
                    it(call, deleteResult)
                }
            } catch (e: Exception) {
                call.respondWithException(e)
            }
        }
    }
}
