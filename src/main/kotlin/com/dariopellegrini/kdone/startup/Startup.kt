package com.dariopellegrini.kdone.startup

import com.dariopellegrini.kdone.application.installKDone
import com.dariopellegrini.kdone.auth.JWTConfig
import com.mongodb.client.MongoDatabase
import io.ktor.routing.Route
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun startKDone(port: Int,
               mongoURL: String,
               jwtConfig: JWTConfig,
               closure: Route.() -> Unit) {
    embeddedServer(Netty, port) {
        installKDone(mongoURL, jwtConfig, closure)
        println("Ready")
    }.start(wait = true)
}

fun startKDone(port: Int,
               mongoDatabase: MongoDatabase,
               jwtConfig: JWTConfig,
               closure: Route.() -> Unit) {
    embeddedServer(Netty, port) {
        installKDone(mongoDatabase, jwtConfig, closure)
        println("Ready")
    }.start(wait = true)
}