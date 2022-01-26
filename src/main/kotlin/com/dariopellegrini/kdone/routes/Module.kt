package com.dariopellegrini.kdone.routes

import auth.mongoId
import auth.userAuth
import auth.userAuthOrNull
import com.dariopellegrini.kdone.annotations.Lookup
import com.dariopellegrini.kdone.application.database
import com.dariopellegrini.kdone.auth.AuthEnum
import com.dariopellegrini.kdone.auth.can
import com.dariopellegrini.kdone.auth.checkPermission
import com.dariopellegrini.kdone.auth.checkToken
import com.dariopellegrini.kdone.constants.*
import com.dariopellegrini.kdone.dto.transferAny
import com.dariopellegrini.kdone.exceptions.BadRequestException
import com.dariopellegrini.kdone.exceptions.ForbiddenException
import com.dariopellegrini.kdone.exceptions.NotFoundException
import com.dariopellegrini.kdone.exceptions.ServerException
import com.dariopellegrini.kdone.extensions.*
import com.dariopellegrini.kdone.languages.localize
import com.dariopellegrini.kdone.model.*
import com.dariopellegrini.kdone.mongo.MongoRepository
import com.dariopellegrini.kdone.websockets.WebSocketController
import com.mongodb.client.model.UnwindOptions
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.request.isMultipart
import io.ktor.request.receive
import io.ktor.request.receiveMultipart
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.util.toMap
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.litote.kmongo.*
import org.litote.kmongo.util.KMongoUtil
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.kotlinProperty
import org.litote.kmongo.MongoOperator.*

inline fun <reified T : Any>Route.module(endpoint: String,
                                         collectionName: String? = null,
                                         configure: RouteConfiguration<T>.() -> Unit = {}) {

    val configuration = RouteConfiguration<T>()
    configuration.configure()
    val repository = MongoRepository(database, collectionName ?: endpoint, T::class.java)
    configuration.repository = repository

    T::class.java.geoIndexJson?.forEach {
        repository.createIndex(it)
    }

    val webSocketController = if (configuration.webSocketActive == true) {
        WebSocketController<T>(repository, endpoint, configuration.authorization).apply {
            configure(this@module)
        }
    } else {
        null
    }
    
    authenticate("jwt", optional = true) {
        get(endpoint) {
            try {
                call.checkToken(this@authenticate.database)
                val userAuth = call.userAuthOrNull
                val shouldCheckOwner = checkPermission(userAuth, configuration.authorization, AuthEnum.READ)

                if (configuration.mongoQueriesDisabled && call.request.queryParameters.contains(queryParameter))
                    throw ForbiddenException("Forbidden parameter")

                // Filters
                val queryMap = mutableMapOf<String, Any>()
                call.request.queryParameters.toMap()
                    .filter { it.key != queryParameter &&
                            it.key != limitParameter &&
                            it.key != skipParameter &&
                            it.key != lookupParameter &&
                            it.key != sortParameter &&
                            it.key != descendingParameter
                    }
                    .map { it.key to it.value.first() }.map { pair ->
                        when {
                            ObjectId.isValid(pair.second) -> queryMap[pair.first] = ObjectId(pair.second)
                            pair.second.toIntOrNull() != null -> queryMap[pair.first] = pair.second.toInt()
                            pair.second.toDoubleOrNull() != null -> queryMap[pair.first] = pair.second.toDouble()
                            pair.second == "true" -> queryMap[pair.first] = true
                            pair.second == "false" -> queryMap[pair.first] = false
                            else -> queryMap[pair.first] = pair.second
                        }
                    }

                if (!queryMap.contains("softDeleted") &&
                    configuration.enableSoftDelete &&
                    T::class.isSubclassOf(SoftDeletable::class)) {
                    queryMap["softDeleted"] = false
                }

                if (queryMap[ignoreSoftDeletedParameter] == true) {
                    queryMap.remove("softDeleted")
                    queryMap.remove(ignoreSoftDeletedParameter)
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

                val limit = call.parameters[limitParameter]?.toIntOrNull()
                val skip = call.parameters[skipParameter]?.toIntOrNull()
                val sortParameter = call.parameters[sortParameter]
                val sortDescending = call.parameters[descendingParameter] != "true"

                val elements = when {
                    configuration.autolookup ||
                            call.request.queryParameters[lookupParameter] == "true" -> {
                        val aggregateList = mutableListOf<Bson>()
                        aggregateList += if (shouldCheckOwner)
                            match(and(
                                Identifiable::owner eq call.userAuth.userId.mongoId(),
                                KMongoUtil.toBson(query)
                            ))
                        else match(KMongoUtil.toBson(query))
                        if (sortParameter != null) {
                            aggregateList += sort(
                                KMongoUtil.toBson("""{$sortParameter:${
                                    if (sortDescending) -1 else 1
                                }}""")
                            )
                        }
                        if (skip != null) {
                            aggregateList += skip(skip)
                        }
                        if (limit != null) {
                            aggregateList += limit(limit)
                        }
                        T::class.java.declaredFields.forEach { field ->
                            field.isAccessible = true
                            if (field.isAnnotationPresent(Lookup::class.java)) {
                                val annotation = field.getAnnotation(Lookup::class.java)
                                aggregateList += lookup(annotation.collectionName, annotation.parameter, annotation.foreignParameter, field.name)
                                val match = annotation.match
                                if (match.isNotEmpty()) {
                                    aggregateList.add(match(KMongoUtil.toBson(match)))
                                }
                                val classifier = field.kotlinProperty?.returnType?.classifier
                                if (classifier != List::class &&
                                    classifier != Array::class &&
                                    classifier != Set::class) {
                                    val options = UnwindOptions()
                                    options.preserveNullAndEmptyArrays(true)
                                    aggregateList += unwind("\$${field.name}", options)
                                }
                            }
                        }
                        repository.aggregateBsonList(aggregateList)
                    }
                    shouldCheckOwner -> repository.findAll(and(
                        Identifiable::owner eq call.userAuth.userId.mongoId(),
                        KMongoUtil.toBson(query)), limit, skip, sortParameter, !sortDescending)
                    else -> repository.findAll(query, limit = limit, skip = skip, sortParameter = sortParameter, ascending = !sortDescending)
                }

                val responseElements = if (configuration.dtoConfiguration != null) {
                    val dtoElements = elements.map {
                        val dtoRead = configuration.dtoConfiguration?.readDTO(userAuth, it) ?: return@map it
                        when {
                            dtoRead.init != null -> dtoRead.init.invoke(it)
                            dtoRead.closure != null -> it.transfer(T::class, dtoRead.kClass, dtoRead.closure)
                            else -> it.transfer(T::class, dtoRead.kClass)
                        }
                    }
                    dtoElements
                } else {
                    elements
                }

                // Localization
                if (call.language != null) {
                    call.respond(HttpStatusCode.OK,
                        if (configuration.useObjectsForArrays) {
                            mapOf("results" to responseElements.map {
                                it.localize(
                                    call.language!!,
                                    configuration.defaultLanguage
                                )
                            })
                        } else {
                            responseElements.map {
                                it.localize(
                                    call.language!!,
                                    configuration.defaultLanguage
                                )
                            }
                        }
                    )
                } else {
                    call.respond(HttpStatusCode.OK,
                        if (configuration.useObjectsForArrays) {
                            mapOf("results" to responseElements)
                        } else {
                            responseElements
                        }
                    )
                }

                configuration.afterGet?.let {
                    it(call,
                        call.request.queryParameters.toMap().map { it.key to it.value.first() }.toMap(),
                        elements)
                }
            } catch (e: Exception) {
                call.respondWithException(e)
                configuration.exceptionHandler?.invoke(call, e)
            }
        }

        get("$endpoint/{id}") {
            try {
                call.checkToken(this@authenticate.database)
                val shouldCheckOwner = checkPermission(call.userAuthOrNull, configuration.authorization, AuthEnum.READ)

                val id = call.parameters["id"] ?: throw BadRequestException("Missing id")

                val element = when {
                    configuration.autolookup ||
                            call.request.queryParameters[lookupParameter] == "true" -> {
                        val aggregateList = mutableListOf<Bson>()
                        aggregateList += match(Identifiable::_id eq id.mongoId())
                        T::class.java.declaredFields.forEach { field ->
                            field.isAccessible = true
                            if (field.isAnnotationPresent(Lookup::class.java)) {
                                val annotation = field.getAnnotation(Lookup::class.java)
                                aggregateList += lookup(
                                    annotation.collectionName,
                                    annotation.parameter,
                                    annotation.foreignParameter,
                                    field.name
                                )
                                val classifier = field.kotlinProperty?.returnType?.classifier
                                if (classifier != List::class &&
                                    classifier != Array::class &&
                                    classifier != Set::class
                                ) {
                                    val options = UnwindOptions()
                                    options.preserveNullAndEmptyArrays(true)
                                    aggregateList += unwind("\$${field.name}", options)
                                }
                            }
                        }
                        repository.aggregateBsonList(aggregateList).firstOrNull()
                            ?: throw NotFoundException("Element with _id $id not found")
                    }
                    else -> repository.findById(id.mongoId())
                }
                if (shouldCheckOwner) {
                    (element as? Identifiable)?.let {
                        if (it.owner.toString() != call.userAuth.userId) throw ForbiddenException()
                    }
                }

                val dtoRead = configuration.dtoConfiguration?.readDTO(call.userAuthOrNull, element)
                val responseElement = if (dtoRead != null) {
                    val dtoElement = when {
                        dtoRead.init != null -> dtoRead.init.invoke(element)
                        dtoRead.closure != null -> element.transfer(T::class, dtoRead.kClass, dtoRead.closure)
                        else -> element.transfer(T::class, dtoRead.kClass)
                    }
                    dtoElement
                } else {
                    element
                }

                // Localization
                if (call.language != null) {
                    call.respond(HttpStatusCode.OK, responseElement.localize(call.language!!, configuration.defaultLanguage))
                } else {
                    call.respond(HttpStatusCode.OK, responseElement)
                }

                configuration.afterGet?.let {
                    it(call,
                        call.request.queryParameters.toMap().map { it.key to it.value.first() }.toMap(),
                        listOf(element))
                }
            } catch (e: Exception) {
                call.respondWithException(e)
                configuration.exceptionHandler?.invoke(call, e)
            }
        }

        post(endpoint) {
            try {
                call.checkToken(this@authenticate.database)
                checkPermission(call.userAuthOrNull, configuration.authorization, AuthEnum.CREATE)

                var element = if (call.request.isMultipart()) {
                    val uploader = configuration.uploader ?: throw ServerException(500, "Uploader not configured")
                    val dtoCreate = configuration.dtoConfiguration?.createDTO(userAuth = call.userAuthOrNull)
                    if (dtoCreate != null) {
                        val klass = dtoCreate.kClass
                        val element = call.receiveMultipart().receive(klass, uploader)
                        when {
                            dtoCreate.init != null -> dtoCreate.init.invoke(element)
                            else -> element.transferAny(klass as KClass<*>, T::class)
                        }
                    } else {
                        call.receiveMultipart().receive(uploader)
                    }
                } else {
                    val dtoCreate = configuration.dtoConfiguration?.createDTO(userAuth = call.userAuthOrNull)
                    if (dtoCreate != null) {
                        val klass = dtoCreate.kClass
                        val element = call.receive(klass)
                        when {
                            dtoCreate.init != null -> dtoCreate.init.invoke(element)
                            else -> element.transferAny(klass as KClass<*>, T::class)
                        }
                    } else {
                        call.receive<T>()
                    }
                }

                // Configure dates
                if (element is DateModel) {
                    element.dateCreated = Date()
                    element.dateUpdated = Date()
                }

                configuration.beforeCreate?.let {
                    it(call, element)
                }

                (element as? Identifiable)?.owner = call.userAuthOrNull?.userId?.mongoId()

                repository.insert(element)

                if (element is Identifiable && (configuration.autolookup || call.parameters[lookupParameter] == "true")) {
                    val aggregateList = mutableListOf<Bson>()
                    aggregateList += match(Identifiable::_id eq element._id)
                    T::class.java.declaredFields.forEach { field ->
                        field.isAccessible = true
                        if (field.isAnnotationPresent(Lookup::class.java)) {
                            val annotation = field.getAnnotation(Lookup::class.java)
                            aggregateList += lookup(
                                annotation.collectionName,
                                annotation.parameter,
                                annotation.foreignParameter,
                                field.name
                            )
                            val classifier = field.kotlinProperty?.returnType?.classifier
                            if (classifier != List::class &&
                                classifier != Array::class &&
                                classifier != Set::class
                            ) {
                                val options = UnwindOptions()
                                options.preserveNullAndEmptyArrays(true)
                                aggregateList += unwind("\$${field.name}", options)
                            }
                        }
                    }
                    element = repository.aggregateBsonList(aggregateList).firstOrNull()
                        ?: element
                }

                val dtoRead = configuration.dtoConfiguration?.readDTO(call.userAuthOrNull, element)
                if (dtoRead != null) {
                    val dtoElement = when {
                        dtoRead.init != null -> dtoRead.init.invoke(element)
                        dtoRead.closure != null -> element.transfer(T::class, dtoRead.kClass, dtoRead.closure)
                        else -> element.transfer(T::class, dtoRead.kClass)
                    }
                    call.respond(HttpStatusCode.Created, dtoElement)
                } else {
                    call.respond(HttpStatusCode.Created, element)
                }

                configuration.afterCreate?.let { it(call, element) }
                webSocketController?.update(element, call.userAuthOrNull)
            } catch (e: Exception) {
                call.respondWithException(e)
                configuration.exceptionHandler?.invoke(call, e)
            }
        }

        patch("$endpoint/{id}") {
            try {
                call.checkToken(this@authenticate.database)
                val shouldCheckOwner = checkPermission(call.userAuthOrNull, configuration.authorization, AuthEnum.UPDATE)

                val id = call.parameters["id"] ?: throw ServerException(400, "Missing id")
                val element = repository.findById(id.mongoId())

                val patch = if (call.request.isMultipart()) {
                    val uploader = configuration.uploader ?: throw ServerException(500, "Uploader not configured")
                    val dtoUpdate = configuration.dtoConfiguration?.updateDTO(call.userAuthOrNull, element)
                    if (dtoUpdate != null) {
                        call.receiveMultipartMap(dtoUpdate.kClass, uploader).apply {
                            checkWithType<T>()
                        }
                    } else {
                        call.receiveMultipartMap<T>(uploader)
                    }
                } else {
                    val dtoUpdate = configuration.dtoConfiguration?.updateDTO(call.userAuthOrNull, element)
                    if (dtoUpdate != null) {
                        call.receiveMap(dtoUpdate.kClass).apply {
                            checkWithType<T>()
                        }
                    } else {
                        call.receiveMap<T>()
                    }
                }.toMutableMap()

                // Configure date
                if (element is DateModel) {
                    element.dateUpdated = Date()
                }

                if (shouldCheckOwner) {
                    (element as? Identifiable)?.owner?.let {
                        if (it.toString() != call.userAuth.userId) throw ForbiddenException()
                    } ?: run { throw ForbiddenException("No owner found") }
                }

                configuration.beforeUpdate?.let {
                    it(call, id.mongoId(), patch)
                }

                repository.updateOneById(id.mongoId(), patch)

                val updatedElement = if (element is Identifiable && (configuration.autolookup || call.parameters[lookupParameter] == "true")) {
                    val aggregateList = mutableListOf<Bson>()
                    aggregateList += match(Identifiable::_id eq element._id)
                    T::class.java.declaredFields.forEach { field ->
                        field.isAccessible = true
                        if (field.isAnnotationPresent(Lookup::class.java)) {
                            val annotation = field.getAnnotation(Lookup::class.java)
                            aggregateList += lookup(
                                annotation.collectionName,
                                annotation.parameter,
                                annotation.foreignParameter,
                                field.name
                            )
                            val classifier = field.kotlinProperty?.returnType?.classifier
                            if (classifier != List::class &&
                                classifier != Array::class &&
                                classifier != Set::class
                            ) {
                                val options = UnwindOptions()
                                options.preserveNullAndEmptyArrays(true)
                                aggregateList += unwind("\$${field.name}", options)
                            }
                        }
                    }
                    repository.aggregateBsonList(aggregateList).firstOrNull() ?: repository.findById(id.mongoId())
                } else {
                    repository.findById(id.mongoId())
                }

                val dtoRead = configuration.dtoConfiguration?.readDTO(call.userAuthOrNull, updatedElement)
                if (dtoRead != null) {
                    val dtoElement = when {
                        dtoRead.init != null -> dtoRead.init.invoke(updatedElement)
                        dtoRead.closure != null -> updatedElement.transfer(T::class, dtoRead.kClass, dtoRead.closure)
                        else -> updatedElement.transfer(T::class, dtoRead.kClass)
                    }
                    call.respond(HttpStatusCode.OK, dtoElement)
                } else {
                    call.respond(HttpStatusCode.OK, updatedElement)
                }

                configuration.afterUpdate?.let {
                    it(call, patch, updatedElement)
                }
                webSocketController?.update(updatedElement, call.userAuthOrNull)
            } catch (e: Exception) {
                call.respondWithException(e)
                configuration.exceptionHandler?.invoke(call, e)
            }
        }

        delete(("$endpoint/{id}")) {
            try {
                call.checkToken(this@authenticate.database)
                val shouldCheckOwner = checkPermission(call.userAuthOrNull, configuration.authorization, AuthEnum.DELETE)

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
                configuration.exceptionHandler?.invoke(call, e)
            }
        }

        if (configuration.optionsEnabled) {
            options(endpoint) {
                try {
                    call.checkToken(this@authenticate.database)
                    val optionsResponse = mutableListOf<OptionsEndpoint>()
                    val authorization = configuration.authorization
                    val parameters = mutableListOf<OptionsEndpoint.OptionsParameter>()
                    val optionalParameters = mutableListOf<OptionsEndpoint.OptionsParameter>()
                    val klass = T::class
                    klass.memberProperties.forEach {
                        if (it.name != "_id" && it.name != "owner" && it.name != "dateCreated" && it.name != "dateUpdated") {
                            parameters.add(
                                OptionsEndpoint.OptionsParameter(
                                    it.name,
                                    it.returnType.toString().cleanClassName(),
                                    it.returnType.isMarkedNullable
                                )
                            )
                            optionalParameters.add(
                                OptionsEndpoint.OptionsParameter(
                                    it.name,
                                    it.returnType.toString().cleanClassName(),
                                    false
                                )
                            )
                        }
                    }

                    if (can(authorization, call.userAuthOrNull, AuthEnum.CREATE)) {
                        optionsResponse.add(OptionsEndpoint(endpoint, "POST", parameters))
                    }
                    if (can(authorization, call.userAuthOrNull, AuthEnum.READ)) {
                        optionsResponse.add(OptionsEndpoint(endpoint, "GET", optionalParameters))
                        optionsResponse.add(OptionsEndpoint("$endpoint/:id", "GET", null))
                    }
                    if (can(authorization, call.userAuthOrNull, AuthEnum.UPDATE)) {
                        optionsResponse.add(OptionsEndpoint("$endpoint/:id", "PATCH", optionalParameters))
                    }
                    if (can(authorization, call.userAuthOrNull, AuthEnum.DELETE)) {
                        optionsResponse.add(OptionsEndpoint("$endpoint/:id", "DELETE", null))
                    }

                    call.respond(HttpStatusCode.OK, optionsResponse)
                } catch (e: Exception) {
                    call.respondWithException(e)
                }
            }
        }
    }
}
