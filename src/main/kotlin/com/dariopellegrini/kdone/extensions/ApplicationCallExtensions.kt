package com.dariopellegrini.kdone.extensions

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
import org.json.simple.JSONObject
import java.io.IOException
import java.util.*
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

suspend inline fun <reified T: Any>ApplicationCall.receiveMap(): Map<String, Any> {
    val inputMap = this.receive<Map<String, Any>>()
    val kClass = T::class
    val propertiesMap = kClass.declaredMemberProperties.map {
        it.name to it
    }.toMap()

    val resultMap = mutableMapOf<String, Any>()

    inputMap.forEach { entry ->
        val key = entry.key
        val value = entry.value
        val property = propertiesMap[key] ?: throw IOException("$key not found for class $kClass")

        when {
            entry.value is Map<*, *> -> {
                val element = ObjectMapper().registerModule(KotlinModule()).readValue<T>(JSONObject((entry.value as Map<*, *>)).toString())
                resultMap[key] = element
            }
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
    beforeUpload: (Map<String, Any>) -> Unit = {}): Map<String, Any> {

    val parts = this@receiveMultipartMap.receiveMultipart().readAllParts()

    val kClass = T::class
    val propertiesMap = kClass.declaredMemberProperties.map {
        it.name to it
    }.toMap()

    val formParts = parts.filterIsInstance<PartData.FormItem>().filter {
        it.name != null
    }.map {
        it.name!!  to when {
            it.value.toIntOrNull() != null -> it.value.toInt()
            it.value.toDoubleOrNull() != null -> it.value.toDouble()
            it.value == "true" -> true
            it.value == "false" -> false
            it.value.dateOrNull != null -> it.value.date
            else -> it.value
        }
    }.toMap().toMutableMap()

    val resultMap = mutableMapOf<String, Any>()

    formParts.forEach { entry ->
        val key = entry.key
        val value = entry.value
        val property = propertiesMap[key] ?: throw IOException("$key not found for class $kClass")

        when {
            entry.value is Map<*, *> -> {
                val element = ObjectMapper().registerModule(KotlinModule()).readValue<T>(JSONObject((entry.value as Map<*, *>)).toString())
                resultMap[key] = element
            }
            property.returnType.jvmErasure.isSubclassOf(value::class) -> resultMap[key] = value
            else -> throw IOException("$key is not instance of ${property.returnType}")
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