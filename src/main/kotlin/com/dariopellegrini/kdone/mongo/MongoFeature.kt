package com.dariopellegrini.kdone.mongo

import com.dariopellegrini.kdone.delegates.Delegate
import com.dariopellegrini.kdone.exceptions.MisconfigurationException
import com.mongodb.client.MongoDatabase
import io.ktor.server.application.ApplicationCall

var ApplicationCall.mongo: MongoDatabase? by Delegate(null)
val ApplicationCall.database: MongoDatabase get() = mongo ?: throw MisconfigurationException("Missing mongo db")