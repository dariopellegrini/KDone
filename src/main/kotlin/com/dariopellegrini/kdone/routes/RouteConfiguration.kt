package com.dariopellegrini.kdone.routes

import com.dariopellegrini.kdone.auth.Authorization
import com.dariopellegrini.kdone.dto.DTOConfiguration
import com.dariopellegrini.kdone.interfaces.RouteActions
import com.dariopellegrini.kdone.uploader.S3Uploader
import com.dariopellegrini.kdone.uploader.Uploader
import com.mongodb.client.result.DeleteResult
import io.ktor.application.ApplicationCall
import io.ktor.util.pipeline.PipelineContext
import org.litote.kmongo.Id
import java.io.File

typealias Headers = Map<String, List<String>>
class RouteConfiguration<T: Any> {
    var authorization: Authorization? = null
    var uploader: Uploader? = null

    var beforeCreate: (suspend (ApplicationCall, T) -> Unit)? = null
    var afterCreate: (suspend (ApplicationCall, T) -> Unit)? = null

    var beforeGet: (suspend (ApplicationCall, Map<String, Any>) -> Unit)? = null
    var afterGet: (suspend (ApplicationCall, Map<String, Any>, List<T>) -> Unit)? = null

    var beforeUpdate: (suspend (ApplicationCall, Id<T>, Map<String, Any>) -> Unit)? = null
    var afterUpdate: (suspend (ApplicationCall, Map<String, Any>, T) -> Unit)? = null

    var beforeDelete: (suspend (ApplicationCall, Id<T>) -> Unit)? = null
    var afterDelete: (suspend (ApplicationCall, DeleteResult) -> Unit)? = null

    var dtoConfiguration: DTOConfiguration<T>? = null

    var exceptionHandler: ((ApplicationCall, Exception) -> Unit)? = null

    fun authorizations(closure: Authorization.() -> Unit) {
        val authorization = Authorization()
        authorization.closure()
        this.authorization = authorization
    }

    fun s3Uploader(baseFolder: String, // "kdone"
                   baseURL: String, // "https://dariopellegrini.ams3.digitaloceanspaces.com"
                   bucketName: String, // "dariopellegrini"
                   accessKey: String, // "2BPF5WP5MVYNGOTYXUDH"
                   secretKey: String, // "eUt3KeIQyeObhI4zXGoW7vrtjuPxrWuo2FJHsBDdJ2M"
                   serviceEndpoint: String, // "https://ams3.digitaloceanspaces.com"
                   signingRegion: String) {
        uploader = S3Uploader(baseFolder, baseURL, bucketName, accessKey, secretKey, serviceEndpoint, signingRegion)
    }

    fun beforeCreate(closure: suspend (ApplicationCall, T) -> Unit) {
        beforeCreate = closure
    }

    fun afterCreate(closure: suspend (ApplicationCall, T) -> Unit) {
        afterCreate = closure
    }

    fun beforeGet(closure: suspend (ApplicationCall, Map<String, Any>) -> Unit) {
        beforeGet = closure
    }

    fun afterGet(closure: suspend (ApplicationCall, Map<String, Any>, List<T>) -> Unit) {
        afterGet = closure
    }

    fun beforeUpdate(closure: suspend (ApplicationCall, Id<T>, Map<String, Any>) -> Unit) {
        beforeUpdate = closure
    }

    fun afterUpdate(closure: suspend (ApplicationCall, Map<String, Any>, T) -> Unit) {
        afterUpdate = closure
    }

    fun beforeDelete(closure: suspend (ApplicationCall, Id<T>) -> Unit) {
        beforeDelete = closure
    }

    fun afterDelete(closure: suspend (ApplicationCall, DeleteResult) -> Unit) {
        afterDelete = closure
    }

    fun dto(closure: DTOConfiguration<T>.() -> Any) {
        dtoConfiguration = DTOConfiguration()
        dtoConfiguration?.closure()
    }

    fun exceptionHandler(closure: (ApplicationCall, Exception) -> Unit) {
        exceptionHandler = closure
    }
}