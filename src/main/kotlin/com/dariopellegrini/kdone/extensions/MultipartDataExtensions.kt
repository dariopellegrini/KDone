package com.dariopellegrini.kdone.extensions

import com.dariopellegrini.kdone.exceptions.ServerException
import com.dariopellegrini.kdone.model.ResourceFile
import com.dariopellegrini.kdone.uploader.Uploader
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.readAllParts
import io.ktor.utils.io.errors.*
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

suspend inline fun <reified T: Any>MultiPartData.receive(
    uploader: Uploader?,
    beforeUpload: (Map<String, String>) -> Unit = {}): T {
    val primaryConstructor = T::class.primaryConstructor ?: throw ServerException(500, "No construsctor")
    val constructorParameters = primaryConstructor.parameters
    val parts = this.readAllParts()

    val resultMap = mutableMapOf<String, Any?>()

    val formParts = parts.filterIsInstance<PartData.FormItem>().filter {
        it.name != null
    }.map {
        resultMap[it.name!!] = it.value
        it.name!! to it.value
    }.toMap().toMutableMap()

    val formProperties = constructorParameters.filter { !it.type.jvmErasure.isSubclassOf(ResourceFile::class) }
    formProperties.forEach {
        val value = formParts[it.name]
        when {
            !it.type.jvmErasure.isSubclassOf(String::class) &&
                    value != null &&
                    (value.trimStart().startsWith("{") && value.trimEnd().endsWith("}") ||
                    value.trimStart().startsWith("[") && value.trimEnd().endsWith("]")) -> {
                resultMap[it.name!!] = ObjectMapper().configureForKDone().readTree(value)
            }
            it.type.jvmErasure.java.isEnum -> {
                val enumValue = it.type.jvmErasure.java.enumConstants.firstOrNull { enumConst ->
                    (enumConst as Enum<*>).name == value
                } ?: throw IOException("Invalid enum value for ${it.name}: $value")
                resultMap[it.name!!] = enumValue
            }
        }
    }

    if (uploader != null) {
        beforeUpload(formParts)

        val fileParts = parts.filterIsInstance<PartData.FileItem>().filter {
            it.name != null
        }.map {
            it.name!!  to it
        }.toMap()

        val fileProperties = constructorParameters.filter { it.type.jvmErasure.isSubclassOf(ResourceFile::class) }
        fileProperties.forEach { kParameter ->
            if (!kParameter.type.isMarkedNullable && fileParts[kParameter.name] == null)
                throw ServerException(400, "Missing ${kParameter.name} property")
            fileParts[kParameter.name]?.let { partData ->
                partData.getFile()?.let { file ->
                    val contentType = partData.contentType.contentTypeString
                    uploader.save(T::class.java.simpleName.toLowerCase(), file.name, file, contentType)?.let {
                        resultMap[kParameter.name!!] = ResourceFile(it, contentType)
                    }
                }
            }
        }
    }
    return ObjectMapper().configureForKDone().convertValue<T>(resultMap)
}

suspend fun <T: Any>MultiPartData.receive(
    klass: KClass<T>,
    uploader: Uploader?,
    beforeUpload: (Map<String, String>) -> Unit = {}): T {
    val primaryConstructor = klass.primaryConstructor ?: throw ServerException(500, "No construsctor")
    val constructorParameters = primaryConstructor.parameters
    val parts = this.readAllParts()

    val resultMap = mutableMapOf<String, Any?>()

    val formParts = parts.filterIsInstance<PartData.FormItem>().filter {
        it.name != null
    }.map {
        resultMap[it.name!!] = it.value
        it.name!! to it.value
    }.toMap().toMutableMap()

    val formProperties = constructorParameters.filter { !it.type.jvmErasure.isSubclassOf(ResourceFile::class) }
    formProperties.forEach {
        val value = formParts[it.name]
        when {
            !it.type.jvmErasure.isSubclassOf(String::class) &&
                    value != null &&
                    (value.trimStart().startsWith("{") && value.trimEnd().endsWith("}") ||
                            value.trimStart().startsWith("[") && value.trimEnd().endsWith("]")) -> {
                resultMap[it.name!!] = ObjectMapper().configureForKDone().readTree(value)
            }
            it.type.jvmErasure.java.isEnum -> {
                val enumValue = it.type.jvmErasure.java.enumConstants.firstOrNull { enumConst ->
                    (enumConst as Enum<*>).name == value
                } ?: throw IOException("Invalid enum value for ${it.name}: $value")
                resultMap[it.name!!] = enumValue
            }
        }
    }

    if (uploader != null) {
        beforeUpload(formParts)

        val fileParts = parts.filterIsInstance<PartData.FileItem>().filter {
            it.name != null
        }.map {
            it.name!!  to it
        }.toMap()

        val fileProperties = constructorParameters.filter { it.type.jvmErasure.isSubclassOf(ResourceFile::class) }
        fileProperties.forEach { kParameter ->
            if (!kParameter.type.isMarkedNullable && fileParts[kParameter.name] == null)
                throw ServerException(400, "Missing ${kParameter.name} property")
            fileParts[kParameter.name]?.let { partData ->
                partData.getFile()?.let { file ->
                    val contentType = partData.contentType.contentTypeString
                    uploader.save(klass.java.simpleName.toLowerCase(), file.name, file, contentType)?.let {
                        resultMap[kParameter.name!!] = ResourceFile(it, contentType)
                    }
                }
            }
        }
    }
    return ObjectMapper().configureForKDone().convertValue(resultMap, klass.java)
}
