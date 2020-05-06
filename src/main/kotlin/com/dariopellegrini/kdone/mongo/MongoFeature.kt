package com.dariopellegrini.kdone.mongo

import com.dariopellegrini.kdone.delegates.Delegate
import com.dariopellegrini.kdone.exceptions.MisconfigurationException
import com.mongodb.client.MongoDatabase
import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.util.AttributeKey

class MongoFeature(configuration: Configuration) {
    val mongo = configuration.mongo // Copies a snapshot of the mutable config into an immutable property.

    class Configuration {
        lateinit var mongo: MongoDatabase
    }

    // Implements ApplicationFeature as a companion object.
    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, MongoFeature> {
        // Creates a unique key for the feature.
        override val key = AttributeKey<MongoFeature>("MongoConfiguration")

        // Code to execute when installing the feature.
        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): MongoFeature {

            // It is responsibility of the install code to call the `configure` method with the mutable configuration.
            val configuration = Configuration().apply(configure)

            // Create the feature, providing the mutable configuration so the feature reads it keeping an immutable copy of the properties.
            val feature = MongoFeature(configuration)



            // Intercept a pipeline.
            pipeline.intercept(ApplicationCallPipeline.Call) {
                // Perform things in that interception point.
                context.mongo = configuration.mongo
            }
            return feature
        }
    }
}

var ApplicationCall.mongo: MongoDatabase? by Delegate(null)
val ApplicationCall.database: MongoDatabase get() = mongo ?: throw MisconfigurationException("Missing mongo db")