package com.dariopellegrini.kdone.extensions

import com.dariopellegrini.kdone.exceptions.ServerException
import com.dariopellegrini.kdone.model.DateModel
import com.dariopellegrini.kdone.model.ResourceFile
import com.dariopellegrini.kdone.uploader.Uploader
import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.readAllParts
import org.litote.kmongo.Id
import org.litote.kmongo.id.IdGenerator
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

suspend inline fun <reified T: Any>MultiPartData.receive(
    uploader: Uploader?,
    beforeUpload: (Map<String, String>) -> Unit = {}): T {
    val primaryConstructor = T::class.primaryConstructor ?: throw ServerException(500, "No construsctor")
    val constructorParameters = primaryConstructor.parameters
    val parts = this.readAllParts()

    val constructorMap: MutableMap<KParameter, Any?> = constructorParameters.map {
        it to null
    }.toMap().toMutableMap()


    val formParts = parts.filterIsInstance<PartData.FormItem>().filter {
        it.name != null
    }.map {
        it.name!!  to it.value
    }.toMap().toMutableMap()

    val formProperties = constructorParameters.filter { !it.type.jvmErasure.isSubclassOf(ResourceFile::class) }
    formProperties.forEach {
//        if (!it.type.isMarkedNullable && formParts[it.name] == null)
//            throw ServerException(400, "Missing ${it.name} property")

        if (T::class.isSubclassOf(DateModel::class) && (it.name == DateModel::dateCreated.name || it.name == DateModel::dateUpdated.name)) {
            constructorMap[it] = Date()
            return@forEach
        }

        val value = formParts[it.name]

        when {
            it.type.jvmErasure.isSubclassOf(Int::class) -> {
                val s = value?.toIntOrNull()
                if (!it.type.isMarkedNullable && s == null) throw ServerException(400, "Property ${it.name} cannot be null")
                constructorMap[it] = s
            }
            it.type.jvmErasure.isSubclassOf(Double::class) -> {
                val s = value?.toDoubleOrNull()
                if (!it.type.isMarkedNullable && s == null) throw ServerException(400, "Property ${it.name} cannot be null")
                constructorMap[it] = s
            }
            it.type.jvmErasure.isSubclassOf(Boolean::class) -> {
                val s = value?.equals("true")
                if (!it.type.isMarkedNullable && s == null) throw ServerException(400, "Property ${it.name} cannot be null")
                constructorMap[it] = s
            }
            it.type.jvmErasure.isSubclassOf(String::class) -> {
                if (!it.type.isMarkedNullable && value == null) throw ServerException(400, "Property ${it.name} cannot be null")
                constructorMap[it] = value
            }
            it.type.jvmErasure.isSubclassOf(Date::class) -> {
                if (!it.type.isMarkedNullable && value == null) throw ServerException(400, "Property ${it.name} cannot be null")
                constructorMap[it] = value?.date
            }
            it.type.jvmErasure.isSubclassOf(Id::class) -> {
                if (!it.type.isMarkedNullable && value == null) throw ServerException(400, "Property ${it.name} cannot be null")
                constructorMap[it] = IdGenerator.defaultGenerator.create(value!!)
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
                        constructorMap[kParameter] = ResourceFile(it, contentType)
                    }
                }
            }
        }
    }

    return primaryConstructor.callBy(constructorMap)
}

suspend fun <T: Any>MultiPartData.receive(
    klass: KClass<T>,
    uploader: Uploader?,
    beforeUpload: (Map<String, String>) -> Unit = {}): T {
    val primaryConstructor = klass.primaryConstructor ?: throw ServerException(500, "No construsctor")
    val constructorParameters = primaryConstructor.parameters
    val parts = this.readAllParts()

    val constructorMap: MutableMap<KParameter, Any?> = constructorParameters.map {
        it to null
    }.toMap().toMutableMap()


    val formParts = parts.filterIsInstance<PartData.FormItem>().filter {
        it.name != null
    }.map {
        it.name!!  to it.value
    }.toMap().toMutableMap()

    val formProperties = constructorParameters.filter { !it.type.jvmErasure.isSubclassOf(ResourceFile::class) }
    formProperties.forEach {
        //        if (!it.type.isMarkedNullable && formParts[it.name] == null)
//            throw ServerException(400, "Missing ${it.name} property")

        if (klass.isSubclassOf(DateModel::class) && (it.name == DateModel::dateCreated.name || it.name == DateModel::dateUpdated.name)) {
            constructorMap[it] = Date()
            return@forEach
        }

        val value = formParts[it.name]

        when {
            it.type.jvmErasure.isSubclassOf(Int::class) -> {
                val s = value?.toIntOrNull()
                if (!it.type.isMarkedNullable && s == null) throw ServerException(400, "Property ${it.name} cannot be null")
                constructorMap[it] = s
            }
            it.type.jvmErasure.isSubclassOf(Double::class) -> {
                val s = value?.toDoubleOrNull()
                if (!it.type.isMarkedNullable && s == null) throw ServerException(400, "Property ${it.name} cannot be null")
                constructorMap[it] = s
            }
            it.type.jvmErasure.isSubclassOf(Boolean::class) -> {
                val s = value?.equals("true")
                if (!it.type.isMarkedNullable && s == null) throw ServerException(400, "Property ${it.name} cannot be null")
                constructorMap[it] = s
            }
            it.type.jvmErasure.isSubclassOf(String::class) -> {
                if (!it.type.isMarkedNullable && value == null) throw ServerException(400, "Property ${it.name} cannot be null")
                constructorMap[it] = value
            }
            it.type.jvmErasure.isSubclassOf(Date::class) -> {
                if (!it.type.isMarkedNullable && value == null) throw ServerException(400, "Property ${it.name} cannot be null")
                constructorMap[it] = value?.date
            }
            it.type.jvmErasure.isSubclassOf(Id::class) -> {
                if (!it.type.isMarkedNullable && value == null) throw ServerException(400, "Property ${it.name} cannot be null")
                constructorMap[it] = IdGenerator.defaultGenerator.create(value!!)
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
                        constructorMap[kParameter] = ResourceFile(it, contentType)
                    }
                }
            }
        }
    }

    return primaryConstructor.callBy(constructorMap)
}
