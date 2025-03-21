package com.dariopellegrini.kdone.startup

import com.dariopellegrini.kdone.application.installKDone
import com.dariopellegrini.kdone.auth.JWTConfig
import com.mongodb.client.MongoDatabase
import io.ktor.server.routing.Route
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.cors.*
import org.litote.kmongo.KMongo

fun startKDone(port: Int,
               mongoURL: String,
               jwtConfig: JWTConfig,
               corsConfig: (CORSConfig.() -> Unit)? = null,
               logsConfig: (CallLoggingConfig.() -> Unit)? = null,
               closure: Route.() -> Unit) {
    embeddedServer(Netty, port) {
        installKDone(mongoURL, jwtConfig, corsConfig, logsConfig, closure)
        println("Ready")
    }.start(wait = true)
}

fun startKDone(port: Int,
               mongoDatabase: MongoDatabase,
               jwtConfig: JWTConfig,
               corsConfig: (CORSConfig.() -> Unit)? = null,
               logsConfig: (CallLoggingConfig.() -> Unit)? = null,
               closure: Route.() -> Unit) {
    embeddedServer(Netty, port) {
        installKDone(mongoDatabase, jwtConfig, corsConfig, logsConfig, closure)
        println("Ready")
    }.start(wait = true)
}

fun startKDone(port: Int,
               mongoURL: String,
               databaseName: String,
               jwtConfig: JWTConfig,
               corsConfig: (CORSConfig.() -> Unit)? = null,
               logsConfig: (CallLoggingConfig.() -> Unit)? = null,
               closure: Route.() -> Unit) {
    embeddedServer(Netty, port) {
//        val settings = MongoClientSettings.builder()
//            .applyConnectionString(ConnectionString(mongoURL))
//            .applyToSslSettings {
//                    builder: SslSettings.Builder -> builder.enabled(true).invalidHostNameAllowed(true)
//            }
//            .build()
        val client = KMongo.createClient(mongoURL)
        installKDone(client.getDatabase(databaseName),
            jwtConfig,
            corsConfig,
            logsConfig,
            closure)
        println("Ready")
    }.start(wait = true)
}