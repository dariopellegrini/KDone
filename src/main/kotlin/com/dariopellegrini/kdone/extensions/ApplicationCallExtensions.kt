package com.dariopellegrini.kdone.extensions

import auth.mongoId
import com.dariopellegrini.kdone.model.DateModel
import com.dariopellegrini.kdone.model.ResourceFile
import com.dariopellegrini.kdone.uploader.Uploader
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.ApplicationCall
import io.ktor.http.content.PartData
import io.ktor.http.content.readAllParts
import io.ktor.request.receive
import io.ktor.request.receiveMultipart
import kotlinx.coroutines.*
import org.bson.types.ObjectId
import org.json.simple.JSONObject
import org.litote.kmongo.Id
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.reflect.KClass
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.jvmErasure

suspend inline fun <reified T: Any>ApplicationCall.receiveMap(): Map<String, Any?> {
    val inputMap = this.receive<Map<String, Any?>>()
    val kClass = T::class
    val propertiesMap = kClass.declaredMemberProperties.map {
        it.name to it
    }.toMap()

    val resultMap = mutableMapOf<String, Any?>()

    inputMap.forEach { entry ->
        val key = entry.key
        val value = entry.value
        val property = propertiesMap[key] ?: throw IOException("$key not found for class $kClass")

        when {
            value == null -> resultMap[key] = null
            entry.value is Map<*, *> -> {
                val element = ObjectMapper().configureForKDone().convertValue(entry.value, property.returnType.jvmErasure.java)
                resultMap[key] = element
            }
            propertiesMap[key]?.javaField?.type == List::class.java && value is ArrayList<*> -> {
                resultMap[key] = value.toList().map {
                        element ->
                    if (element is String && ObjectId.isValid(element)) {
                        element.mongoId<Any>()
                    } else {
                        element
                    }
                }
//                resultMap[key] = value.toList()
            }
            property.returnType.jvmErasure.isSubclassOf(Id::class) && value is String && ObjectId.isValid(value) -> resultMap[key] = value.mongoId<Any>()
            property.returnType.jvmErasure.isSubclassOf(value::class) -> resultMap[key] = value
            else -> throw IOException("$key is not instance of ${property.returnType}")
        }
    }

    if (DateModel::class.java.isAssignableFrom(T::class.java)) {
        resultMap.remove("dateCreated")
        resultMap.put("dateUpdated", Date())
    }

    return resultMap
}

suspend inline fun <reified T: Any>ApplicationCall.receiveMultipartMap(
    uploader: Uploader,
    addUnknown: List<String> = listOf(),
    beforeUpload: (Map<String, Any>) -> Unit = {}): Map<String, Any?> {

    val parts = this@receiveMultipartMap.receiveMultipart().readAllParts()

    val kClass = T::class
    val propertiesMap = kClass.declaredMemberProperties.map {
        it.name to it
    }.toMap()

    val formParts = parts.filterIsInstance<PartData.FormItem>().filter {
        it.name != null
    }.map {
        it.name!! to when {
            propertiesMap[it.name]?.returnType?.jvmErasure?.isSubclassOf(Int::class) == true && it.value.toIntOrNull() != null -> it.value.toInt()
            propertiesMap[it.name]?.returnType?.jvmErasure?.isSubclassOf(Double::class) == true && it.value.toDoubleOrNull() != null -> it.value.toDouble()
            propertiesMap[it.name]?.returnType?.jvmErasure?.isSubclassOf(Boolean::class) == true && it.value == "true" -> true
            propertiesMap[it.name]?.returnType?.jvmErasure?.isSubclassOf(Boolean::class) == true && it.value == "false" -> false
            propertiesMap[it.name]?.returnType?.jvmErasure?.isSubclassOf(Date::class) == true && it.value.dateOrNull != null -> it.value.date
            else -> it.value
        }
    }.toMap().toMutableMap()

    val resultMap = mutableMapOf<String, Any>()

    formParts.forEach { entry ->
        val key = entry.key
        val value = entry.value
        if (!addUnknown.contains(key)) {
            val property = propertiesMap[key] ?: throw IOException("$key not found for class $kClass")
            when {
                entry.value is Map<*, *> -> {
                    val element = ObjectMapper().configureForKDone().convertValue(entry.value, property.returnType.jvmErasure.java)
                    resultMap[key] = element
                }
                property.returnType.jvmErasure.isSubclassOf(List::class) && entry.value is String -> {
                    val element = ObjectMapper().configureForKDone().readValue(entry.value.toString(), ArrayList::class.java)
                    val listInnerType = property.returnType.arguments.firstOrNull()?.type
                    if (listInnerType != null && listInnerType.jvmErasure.isSubclassOf(Id::class) && element is ArrayList<*>) {
                        val list = element.toMutableList()
                        list.indices.forEach { i ->
                            val elementValue = element[i]
                            if (elementValue is String && ObjectId.isValid(elementValue)) {
                                list[i] = elementValue.mongoId<Any>()
                            }
                        }
                        resultMap[key] = list
                    } else {
                        resultMap[key] = element
                    }
                }
                value is String && (value.trimStart().startsWith("{") && value.trimEnd().endsWith("}") ||
                        value.trimStart().startsWith("[") && value.trimEnd().endsWith("]")) -> {
                    resultMap[key] = ObjectMapper().configureForKDone().readValue(value, property.returnType.jvmErasure.java)
                }
                property.returnType.jvmErasure.isSubclassOf(Id::class) && value is String && ObjectId.isValid(value) -> resultMap[key] = value.mongoId<Any>()
                property.returnType.jvmErasure.isSubclassOf(value::class) -> resultMap[key] = value
                else -> throw IOException("$key is not instance of ${property.returnType}")
            }
        } else {
            resultMap[key] = value
        }
    }

    if (DateModel::class.java.isAssignableFrom(T::class.java)) {
        resultMap.remove("dateCreated")
        resultMap["dateUpdated"] = Date()
    }

    beforeUpload(resultMap)

    parts.filterIsInstance<PartData.FileItem>().filter {
        it.name != null
    }.map {
        it.name!!  to it
    }.toMap().map { entry ->
        val key = entry.key
        val value = entry.value
        val property = propertiesMap[key] ?: throw IOException("$key not found for class $kClass")

        if (property.returnType.jvmErasure.isSubclassOf(ResourceFile::class)) {
            value.getFile()?.let { file ->
                val contentType = value.contentType.contentTypeString
                uploader.save(T::class.java.simpleName.toLowerCase(), file.name, file, contentType)?.let {
                    resultMap[property.name] = ResourceFile(it, contentType)
                }
            }
        }
    }

//    jobs.awaitAll()

    return resultMap
}

suspend fun <T: Any>ApplicationCall.receiveMap(kClass: KClass<T>): Map<String, Any?> {
    val inputMap = this.receive<Map<String, Any>>()
    val propertiesMap = kClass.declaredMemberProperties.map {
        it.name to it
    }.toMap()

    val resultMap = mutableMapOf<String, Any?>()

    inputMap.forEach { entry ->
        val key = entry.key
        val value = entry.value
        val property = propertiesMap[key] ?: throw IOException("$key not found for class $kClass")


        when {
            value == null -> resultMap[key] = null
            entry.value is Map<*, *> -> {
                val element = ObjectMapper().configureForKDone().convertValue(entry.value, property.returnType.jvmErasure.java)
                resultMap[key] = element
            }
            property.returnType.jvmErasure.isSubclassOf(List::class) && entry.value is String -> {
                val element = ObjectMapper().configureForKDone().readValue(entry.value.toString(), ArrayList::class.java)
                val listInnerType = property.returnType.arguments.firstOrNull()?.type
                if (listInnerType != null && listInnerType.jvmErasure.isSubclassOf(Id::class) && element is ArrayList<*>) {
                    val list = element.toMutableList()
                    list.indices.forEach { i ->
                        val elementValue = element[i]
                        if (elementValue is String && ObjectId.isValid(elementValue)) {
                            list[i] = elementValue.mongoId<Any>()
                        }
                    }
                    resultMap[key] = list
                } else {
                    resultMap[key] = element
                }
            }
            value is String && (value.trimStart().startsWith("{") && value.trimEnd().endsWith("}") ||
                    value.trimStart().startsWith("[") && value.trimEnd().endsWith("]")) -> {
                resultMap[key] = ObjectMapper().configureForKDone().readValue(value, property.returnType.jvmErasure.java)
            }
            property.returnType.jvmErasure.isSubclassOf(Id::class) && value is String && ObjectId.isValid(value) -> resultMap[key] = value.mongoId<Any>()
            property.returnType.jvmErasure.isSubclassOf(List::class) && value is List<*> -> resultMap[key] = value.map {
                element ->
                if (element is String && ObjectId.isValid(element)) {
                    element.mongoId<Any>()
                } else {
                    element
                }
            }
            property.returnType.jvmErasure.isSubclassOf(value::class) -> resultMap[key] = value
            else -> throw IOException("$key is not instance of ${property.returnType}")
        }
    }

    if (DateModel::class.java.isAssignableFrom(kClass.java)) {
        resultMap.remove("dateCreated")
        resultMap.put("dateUpdated", Date())
    }

    return resultMap
}

suspend fun <T: Any>ApplicationCall.receiveMultipartMap(
    kClass: KClass<T>,
    uploader: Uploader,
    addUnknown: List<String> = listOf(),
    beforeUpload: (Map<String, Any>) -> Unit = {}): Map<String, Any> {
    val parts = this@receiveMultipartMap.receiveMultipart().readAllParts()
    val propertiesMap = kClass.declaredMemberProperties.map {
        it.name to it
    }.toMap()

    val formParts = parts.filterIsInstance<PartData.FormItem>().filter {
        it.name != null
    }.map {
        it.name!! to when {
            propertiesMap[it.name]?.returnType?.jvmErasure?.isSubclassOf(Int::class) == true && it.value.toIntOrNull() != null -> it.value.toInt()
            propertiesMap[it.name]?.returnType?.jvmErasure?.isSubclassOf(Double::class) == true && it.value.toDoubleOrNull() != null -> it.value.toDouble()
            propertiesMap[it.name]?.returnType?.jvmErasure?.isSubclassOf(Boolean::class) == true && it.value == "true" -> true
            propertiesMap[it.name]?.returnType?.jvmErasure?.isSubclassOf(Boolean::class) == true && it.value == "false" -> false
            propertiesMap[it.name]?.returnType?.jvmErasure?.isSubclassOf(Date::class) == true && it.value.dateOrNull != null -> it.value.date
            else -> it.value
        }
    }.toMap().toMutableMap()

    val resultMap = mutableMapOf<String, Any>()

    formParts.forEach { entry ->
        val key = entry.key
        val value = entry.value
        if (!addUnknown.contains(key)) {
            val property = propertiesMap[key] ?: throw IOException("$key not found for class $kClass")

            when {
                entry.value is Map<*, *> -> {
                    val element = ObjectMapper().configureForKDone().convertValue(entry.value, property.returnType.jvmErasure.java)
                    resultMap[key] = element
                }
                property.returnType.jvmErasure.isSubclassOf(List::class) && entry.value is String -> {
                    val element = ObjectMapper().configureForKDone().readValue(entry.value.toString(), ArrayList::class.java)
                    val listInnerType = property.returnType.arguments.firstOrNull()?.type
                    if (listInnerType != null && listInnerType.jvmErasure.isSubclassOf(Id::class) && element is ArrayList<*>) {
                        val list = element.toMutableList()
                        list.indices.forEach { i ->
                            val elementValue = element[i]
                            if (elementValue is String && ObjectId.isValid(elementValue)) {
                                list[i] = elementValue.mongoId<Any>()
                            }
                        }
                        resultMap[key] = list
                    } else {
                        resultMap[key] = element
                    }
                }
                property.returnType.jvmErasure.isSubclassOf(List::class) && entry.value is List<*> -> {
                    val element = ObjectMapper().configureForKDone().convertValue(entry.value, property.returnType.jvmErasure.java)
                    resultMap[key] = element
                }
                property.returnType.jvmErasure.isSubclassOf(Id::class) && value is String && ObjectId.isValid(value) -> resultMap[key] = value.mongoId<Any>()
                property.returnType.jvmErasure.isSubclassOf(value::class) -> resultMap[key] = value
                else -> throw IOException("$key is not instance of ${property.returnType}")
            }
        } else {
            resultMap[key] = value
        }
    }

    if (DateModel::class.java.isAssignableFrom(kClass.java)) {
        resultMap.remove("dateCreated")
        resultMap["dateUpdated"] = Date()
    }

    beforeUpload(resultMap)

    parts.filterIsInstance<PartData.FileItem>().filter {
        it.name != null
    }.map {
        it.name!!  to it
    }.toMap().map { entry ->
        val key = entry.key
        val value = entry.value
        val property = propertiesMap[key] ?: throw IOException("$key not found for class $kClass")

        if (property.returnType.jvmErasure.isSubclassOf(ResourceFile::class)) {
            value.getFile()?.let { file ->
                val contentType = value.contentType.contentTypeString
                uploader.save(kClass.java.simpleName.toLowerCase(), file.name, file, contentType)?.let {
                    resultMap[property.name] = ResourceFile(it, contentType)
                }
            }
        }
    }

//    jobs.awaitAll()

    return resultMap
}

val ApplicationCall.language get() = request.headers["Accept-Language"]
