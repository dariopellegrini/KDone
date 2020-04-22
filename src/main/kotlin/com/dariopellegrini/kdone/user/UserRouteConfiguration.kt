package com.dariopellegrini.kdone.user

import com.dariopellegrini.kdone.auth.UserAuthorization
import com.dariopellegrini.kdone.email.EmailConfirmationConfiguration
import com.dariopellegrini.kdone.email.EmailSender
import com.dariopellegrini.kdone.email.model.EmailMessage
import com.dariopellegrini.kdone.uploader.S3Uploader
import com.dariopellegrini.kdone.uploader.Uploader
import com.dariopellegrini.kdone.user.hash.HashStrategy
import com.dariopellegrini.kdone.user.model.KDoneUser
import com.dariopellegrini.kdone.user.model.LoginInput
import com.dariopellegrini.kdone.user.social.apple.AppleConfiguration
import com.dariopellegrini.kdone.user.social.facebook.FacebookConfiguration
import com.dariopellegrini.kdone.user.social.google.GoogleConfiguration
import com.mongodb.client.result.DeleteResult
import io.ktor.application.ApplicationCall
import org.litote.kmongo.Id
import org.simplejavamail.mailer.internal.MailerRegularBuilderImpl

open class UserRouteConfiguration<T: KDoneUser> {
    var authorization: UserAuthorization = UserAuthorization()
    var uploader: Uploader? = null

    var beforeLogin: (suspend (ApplicationCall, LoginInput) -> Unit)? = null
    var afterLogin: (suspend (ApplicationCall, LoginInput) -> Unit)? = null

    var beforeLogout: (suspend (ApplicationCall) -> Unit)? = null
    var afterLogout: (suspend (ApplicationCall) -> Unit)? = null

    var beforeCreate: (suspend (ApplicationCall, T) -> Unit)? = null
    var afterCreate: (suspend (ApplicationCall, T) -> Unit)? = null

    var beforeGet: (suspend (ApplicationCall, Map<String, Any>) -> Unit)? = null
    var afterGet: (suspend (ApplicationCall, Map<String, Any>, List<T>) -> Unit)? = null

    var beforeUpdate: (suspend (ApplicationCall, Id<T>, Map<String, Any>) -> Unit)? = null
    var afterUpdate: (suspend (ApplicationCall, Map<String, Any>, T) -> Unit)? = null

    var beforeDelete: (suspend (ApplicationCall, Id<T>) -> Unit)? = null
    var afterDelete: (suspend (ApplicationCall, DeleteResult) -> Unit)? = null

    var facebook: FacebookConfiguration? = null
    var apple: AppleConfiguration? = null
    var google: GoogleConfiguration? = null

    var hashStrategy: HashStrategy? = null

    var exceptionHandler: ((ApplicationCall, Exception) -> Unit)? = null

    var loggedInAfterSignUp: Boolean? = null

    var needsEmailConfirmation: Boolean? = null
    var emailConfirmationConfiguration: EmailConfirmationConfiguration? = null

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

    fun beforeLogin(closure: suspend (ApplicationCall, LoginInput) -> Unit) {
        beforeLogin = closure
    }

    fun afterLogin(closure: suspend (ApplicationCall, LoginInput) -> Unit) {
        afterLogin = closure
    }

    fun beforeLogout(closure: suspend (ApplicationCall) -> Unit) {
        beforeLogout = closure
    }

    fun afterLogout(closure: suspend (ApplicationCall) -> Unit) {
        afterLogout = closure
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

    fun facebook(appId: String, appSecret: String) {
        facebook =
            FacebookConfiguration(appId, appSecret)
    }

    fun apple(bundleId: String) {
        apple = AppleConfiguration(bundleId)
    }

    fun google(clientId: String, clientSecret: String, redirectURL: String) {
        google = GoogleConfiguration(clientId, clientSecret, redirectURL)
    }

    fun hashStrategy(hash: (String) -> String, verify: (String, String) -> Boolean) {
        hashStrategy = HashStrategy(hash, verify)
    }

    fun exceptionHandler(closure: (ApplicationCall, Exception) -> Unit) {
        exceptionHandler = closure
    }

    fun emailConfirmation(emailSender: EmailSender,
                          baseURL: String,
                          redirectURL: String,
                          emailSenderClosure: (String) -> EmailMessage) {
        emailConfirmationConfiguration = EmailConfirmationConfiguration(emailSender, baseURL, redirectURL, emailSenderClosure)
    }
}