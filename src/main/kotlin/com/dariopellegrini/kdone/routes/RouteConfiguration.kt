package com.dariopellegrini.kdone.routes

import com.dariopellegrini.kdone.auth.Authorization
import com.dariopellegrini.kdone.dto.DTOConfiguration
import com.dariopellegrini.kdone.mongo.MongoRepository
import com.dariopellegrini.kdone.uploader.S3Uploader
import com.dariopellegrini.kdone.uploader.Uploader
import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.UpdateResult
import io.ktor.application.ApplicationCall
import org.litote.kmongo.Id
import java.io.File

class RouteConfiguration<T: Any> {
    var authorization: Authorization? = null
    var uploader: Uploader? = null

    var beforeCreate: (suspend (ApplicationCall, T) -> Unit)? = null
    var afterCreate: (suspend (ApplicationCall, T) -> Unit)? = null

    var beforeGet: (suspend (ApplicationCall, MutableMap<String, Any>) -> Unit)? = null
    var afterGet: (suspend (ApplicationCall, Map<String, Any>, List<T>) -> Unit)? = null

    var beforeUpdate: (suspend (ApplicationCall, Id<T>, MutableMap<String, Any?>) -> Unit)? = null
    var afterUpdate: (suspend (ApplicationCall, Map<String, Any?>, T) -> Unit)? = null

    var beforeDelete: (suspend (ApplicationCall, Id<T>) -> Unit)? = null
    var afterDelete: (suspend (ApplicationCall, DeleteResult) -> Unit)? = null
    var afterSoftDelete: (suspend (ApplicationCall, UpdateResult) -> Unit)? = null

    var dtoConfiguration: DTOConfiguration<T>? = null

    var exceptionHandler: ((ApplicationCall, Exception) -> Unit)? = null

    var mongoQueriesDisabled = false

    var defaultLanguage = "en"

    var optionsEnabled = false

    var webSocketActive = false

    var autolookup = false

    var enableSoftDelete = false

    var useObjectsForArrays = false

    fun authorizations(closure: Authorization.() -> Unit) {
        val authorization = Authorization()
        authorization.closure()
        this.authorization = authorization
    }

    fun s3Uploader(baseFolder: String,
                   baseURL: String,
                   bucketName: String,
                   accessKey: String,
                   secretKey: String,
                   serviceEndpoint: String,
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

    fun beforeUpdate(closure: suspend (ApplicationCall, Id<T>, MutableMap<String, Any?>) -> Unit) {
        beforeUpdate = closure
    }

    fun afterUpdate(closure: suspend (ApplicationCall, Map<String, Any?>, T) -> Unit) {
        afterUpdate = closure
    }

    fun beforeDelete(closure: suspend (ApplicationCall, Id<T>) -> Unit) {
        beforeDelete = closure
    }

    fun afterDelete(closure: suspend (ApplicationCall, DeleteResult) -> Unit) {
        afterDelete = closure
    }

    fun afterSoftDelete(closure: suspend (ApplicationCall, UpdateResult) -> Unit) {
        afterSoftDelete = closure
    }

    fun dto(closure: DTOConfiguration<T>.() -> Any) {
        dtoConfiguration = DTOConfiguration()
        dtoConfiguration?.closure()
    }

    fun exceptionHandler(closure: (ApplicationCall, Exception) -> Unit) {
        exceptionHandler = closure
    }

    lateinit var repository: MongoRepository<T>
}