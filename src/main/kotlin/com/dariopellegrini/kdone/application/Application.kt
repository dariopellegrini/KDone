package com.dariopellegrini.kdone.application

import com.dariopellegrini.kdone.auth.JWTValidator
import com.dariopellegrini.kdone.auth.JWTConfig
import com.dariopellegrini.kdone.delegates.Delegate
import com.dariopellegrini.kdone.exceptions.MisconfigurationException
import com.dariopellegrini.kdone.extensions.configureForKDone
import com.dariopellegrini.kdone.mongo.MongoRepository
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.jwt
import io.ktor.features.*
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.jackson.jackson
import io.ktor.routing.Route
import io.ktor.routing.Routing
import io.ktor.routing.routing
import io.ktor.websocket.*
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
        header(HttpHeaders.AcceptLanguage)
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
    install(WebSockets)

    routing {
        mongo = mongoDatabase
        jwtConfigDelegate = jwtConfig

        configureRoutes()
    }
}

inline fun <reified T>Route.mongoRepository(collectionName: String): MongoRepository<T> {
    return MongoRepository(this.database, collectionName, T::class.java)
}

fun Route.mongoCollection(collectionName: String): MongoCollection<Document> {
    return database.getCollection(collectionName)
}

var Route.mongo: MongoDatabase? by Delegate(null)
val Route.database: MongoDatabase get() = mongo ?: throw MisconfigurationException("Missing mongo db")
var Route.jwtConfigDelegate: JWTConfig? by Delegate(null)
val Route.jwtConfiguration: JWTConfig get() = jwtConfigDelegate ?: throw MisconfigurationException("Missing mongo db")

fun Route.authenticateJWT(optional: Boolean = false,
                          build: Route.() -> Unit) = authenticate("jwt", optional = optional, build = build)

fun Application.installKDone(mongoURL: String, jwtConfig: JWTConfig, configureRoutes: Routing.() -> Unit) {
    installKDone(KMongo.createClient(mongoURL.substring(0, mongoURL.lastIndexOf("/")))
        .getDatabase(mongoURL.substring(mongoURL.lastIndexOf("/") + 1)), jwtConfig, configureRoutes)
}
