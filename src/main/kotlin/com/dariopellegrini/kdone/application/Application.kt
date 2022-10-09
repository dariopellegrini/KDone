package com.dariopellegrini.kdone.application

import com.dariopellegrini.kdone.auth.JWTValidator
import com.dariopellegrini.kdone.auth.JWTConfig
import com.dariopellegrini.kdone.delegates.Delegate
import com.dariopellegrini.kdone.exceptions.MisconfigurationException
import com.dariopellegrini.kdone.extensions.configureForKDone
import com.dariopellegrini.kdone.mongo.KDoneMongoContainer
import com.dariopellegrini.kdone.mongo.MongoRepository
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import io.ktor.server.application.*
import io.ktor.server.auth.*import io.ktor.server.auth.jwt.jwt
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.jackson.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.routing.Route
import io.ktor.server.routing.Routing
import io.ktor.server.routing.routing
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import io.ktor.websocket.*
import org.bson.Document
import org.litote.kmongo.KMongo
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.forwardedheaders.*
import io.ktor.server.websocket.*

fun Application.installKDone(mongoDatabase: MongoDatabase,
                             jwtConfig: JWTConfig,
                             corsConfig: (CORSConfig.() -> Unit)? = null,
                             configureRoutes: Routing.() -> Unit) {
    KDoneMongoContainer.database = mongoDatabase

    install(DefaultHeaders)
    install(ForwardedHeaders)
    install(CallLogging)
    install(CORS) {
        if (corsConfig != null) {
            corsConfig(this)
        } else {
            anyHost()
        }

        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.AccessControlAllowOrigin)
        allowHeader(HttpHeaders.AccessControlAllowHeaders)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.AcceptLanguage)
        allowHeader("facebookToken")
        allowHeader("facebookId")
        allowHeader("appleToken")
        allowHeader("appleId")
        allowHeader("googleToken")
        allowHeader("googleId")
        exposeHeader(HttpHeaders.Authorization)
        allowNonSimpleContentTypes = true
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
            realm = jwtConfig.issuer
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

val database: MongoDatabase get() = KDoneMongoContainer.database ?: throw MisconfigurationException("Missing mongo db")
inline fun <reified T>mongoRepository(collectionName: String): MongoRepository<T> {
    return MongoRepository(database, collectionName, T::class.java)
}
inline fun <reified T>mongoCollection(collectionName: String) = database.getCollection(collectionName, T::class.java)

fun Route.authenticateJWT(optional: Boolean = false,
                          build: Route.() -> Unit) = authenticate("jwt", optional = optional, build = build)

fun Application.installKDone(mongoURL: String,
                             jwtConfig: JWTConfig,
                             corsConfig: (CORSConfig.() -> Unit)? = null,
                             configureRoutes: Routing.() -> Unit) {
    installKDone(KMongo.createClient(mongoURL.substring(0, mongoURL.lastIndexOf("/")))
        .getDatabase(mongoURL.substring(mongoURL.lastIndexOf("/") + 1)), jwtConfig, corsConfig, configureRoutes)
}
