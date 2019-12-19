package com.dariopellegrini.kdone.user

import com.dariopellegrini.kdone.auth.UserAuthorization
import com.dariopellegrini.kdone.routes.Headers
import com.dariopellegrini.kdone.uploader.S3Uploader
import com.dariopellegrini.kdone.uploader.Uploader
import com.dariopellegrini.kdone.user.model.KDoneUser
import com.dariopellegrini.kdone.user.model.LoginInput
import com.dariopellegrini.kdone.user.social.apple.AppleConfiguration
import com.dariopellegrini.kdone.user.social.facebook.FacebookConfiguration
import com.mongodb.client.result.DeleteResult
import org.litote.kmongo.Id

open class UserRouteConfiguration<T: KDoneUser> {
    var authorization: UserAuthorization = UserAuthorization()
    var uploader: Uploader? = null

    var beforeLogin: (suspend (Headers, LoginInput) -> Unit)? = null
    var afterLogin: (suspend (Headers, LoginInput) -> Unit)? = null

    var beforeLogout: (suspend (Headers) -> Unit)? = null
    var afterLogout: (suspend (Headers) -> Unit)? = null

    var beforeCreate: (suspend (Headers, T) -> Unit)? = null
    var afterCreate: (suspend (Headers, T) -> Unit)? = null

    var beforeGet: (suspend (Headers, Map<String, Any>) -> Unit)? = null
    var afterGet: (suspend (Headers, Map<String, Any>, List<T>) -> Unit)? = null

    var beforeUpdate: (suspend (Headers, Id<T>, Map<String, Any>) -> Unit)? = null
    var afterUpdate: (suspend (Headers, Map<String, Any>, T) -> Unit)? = null

    var beforeDelete: (suspend (Headers, Id<T>) -> Unit)? = null
    var afterDelete: (suspend (Headers, DeleteResult) -> Unit)? = null

    var facebook: FacebookConfiguration? = null
    var apple: AppleConfiguration? = null

    fun authorizations(closure: UserAuthorization.() -> Unit) {
        authorization.closure()
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

    fun beforeLogin(closure: suspend (Headers, LoginInput) -> Unit) {
        beforeLogin = closure
    }

    fun afterLogin(closure: suspend (Headers, LoginInput) -> Unit) {
        afterLogin = closure
    }

    fun beforeLogout(closure: suspend (Headers) -> Unit) {
        beforeLogout = closure
    }

    fun afterLogout(closure: suspend (Headers) -> Unit) {
        afterLogout = closure
    }

    fun beforeCreate(closure: suspend (Headers, T) -> Unit) {
        beforeCreate = closure
    }

    fun afterCreate(closure: suspend (Headers, T) -> Unit) {
        afterCreate = closure
    }

    fun beforeGet(closure: suspend (Headers, Map<String, Any>) -> Unit) {
        beforeGet = closure
    }

    fun afterGet(closure: suspend (Headers, Map<String, Any>, List<T>) -> Unit) {
        afterGet = closure
    }

    fun beforeUpdate(closure: suspend (Headers, Id<T>, Map<String, Any>) -> Unit) {
        beforeUpdate = closure
    }

    fun afterUpdate(closure: suspend (Headers, Map<String, Any>, T) -> Unit) {
        afterUpdate = closure
    }

    fun beforeDelete(closure: suspend (Headers, Id<T>) -> Unit) {
        beforeDelete = closure
    }

    fun afterDelete(closure: suspend (Headers, DeleteResult) -> Unit) {
        afterDelete = closure
    }

    fun facebook(appId: String, appSecret: String) {
        facebook =
            FacebookConfiguration(appId, appSecret)
    }

    fun apple(bundleId: String) {
        apple = AppleConfiguration(bundleId)
    }
}