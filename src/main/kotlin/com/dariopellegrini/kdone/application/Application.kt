package com.dariopellegrini.kdone.application

import com.dariopellegrini.kdone.auth.JWTValidator
import com.dariopellegrini.kdone.auth.JWTConfig
import com.dariopellegrini.kdone.delegates.Delegate
import com.dariopellegrini.kdone.exceptions.MisconfigurationException
import com.dariopellegrini.kdone.extensions.configureForKDone
import com.dariopellegrini.kdone.mongo.MongoRepository
import com.mongodb.MongoClientURI
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import io.ktor.application.*
import io.ktor.auth.Authentication
import io.ktor.auth.jwt.jwt
import io.ktor.features.*
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.jackson.jackson
import io.ktor.routing.Route
import io.ktor.routing.Routing
import io.ktor.routing.routing
import org.bson.Document
import org.litote.kmongo.KMongo

fun Application.installKDone(mongoDatabase: MongoDatabase,
                             jwtConfig: JWTConfig,
                             configureRoutes: Routing.() -> Unit) {
    install(DefaultHeaders)
    install(ForwardedHeaderSupport)
    install(CallLogging)
    install(CORS) {
        anyHost()
        method(HttpMethod.Options)
        method(HttpMethod.Get)
        method(HttpMethod.Post)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
        header(HttpHeaders.Authorization)
        header(HttpHeaders.AccessControlAllowOrigin)
        header(HttpHeaders.AccessControlAllowHeaders)
        header(HttpHeaders.ContentType)
        header("facebookToken")
        header("facebookId")
        exposeHeader(HttpHeaders.Authorization)
        allowNonSimpleContentTypes = true
        anyHost()
    }
    install(ContentNegotiation) {

        jackson {
            configureForKDone()
        }
    }

    install(Authentication) {
        jwt("jwt") {
            verifier {
                jwtConfig.verifier
            }
            realm = "kdone.dariopellegrini.com"
            validate { credentials ->
                JWTValidator().validate(credentials)
            }
        }
    }

    routing {
        mongo = mongoDatabase
        jwtConfigDelegate = jwtConfig

        configureRoutes()
    }
}

var Route.mongo: MongoDatabase? by Delegate(null)
val Route.database: MongoDatabase get() = mongo ?: throw MisconfigurationException("Missing mongo db")

inline fun <reified T>Route.mongoRepository(collectionName: String): MongoRepository<T> {
    return MongoRepository(database, collectionName, T::class.java)
}

fun Route.mongoCollection(collectionName: String): MongoCollection<Document> {
    return mongo!!.getCollection(collectionName)
}

var Route.jwtConfigDelegate: JWTConfig? by Delegate(null)
val Route.jwtConfiguration: JWTConfig get() = jwtConfigDelegate ?: throw MisconfigurationException("Missing mongo db")

fun Application.installKDone(mongoURL: String, jwtConfig: JWTConfig, configureRoutes: Routing.() -> Unit) {
    installKDone(KMongo.createClient(MongoClientURI(mongoURL.substring(0, mongoURL.lastIndexOf("/"))))
        .getDatabase(mongoURL.substring(mongoURL.lastIndexOf("/") + 1)), jwtConfig, configureRoutes)
}