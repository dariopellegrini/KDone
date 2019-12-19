package com.dariopellegrini.kdone

import com.dariopellegrini.kdone.auth.*
import com.dariopellegrini.kdone.configuration.Apple
import com.dariopellegrini.kdone.configuration.Facebook
import com.dariopellegrini.kdone.configuration.JWT
import com.dariopellegrini.kdone.configuration.S3
import com.dariopellegrini.kdone.model.DateModel
import com.dariopellegrini.kdone.model.Identifiable
import com.dariopellegrini.kdone.model.ResourceFile
import com.dariopellegrini.kdone.routes.module
import com.dariopellegrini.kdone.startup.startKDone
import com.dariopellegrini.kdone.uploader.S3Uploader
import com.dariopellegrini.kdone.user.model.KDoneUser
import com.dariopellegrini.kdone.user.userModule
import com.fasterxml.jackson.annotation.JsonInclude
import java.util.*

fun main() {
    startKDone(
        port = System.getenv("PORT")?.toInt() ?: 23146,
        mongoURL = "mongodb://localhost:27017/games",
        jwtConfig = JWTConfig(JWT.secret)) {

        val s3Uploader = S3Uploader(
            baseFolder = S3.baseFolder,
            baseURL = S3.baseURL,
            bucketName = S3.bucketName,
            accessKey = S3.accessKey,
            secretKey = S3.secretKey,
            serviceEndpoint = S3.serviceEndpoint,
            signingRegion = S3.signingRegion
        )

        module<Game>("games") {
            authorizations {
                guest()
                registered(create, read)
                owner(read, update, delete)
                "admin".can(create, read, update, delete)
                "officer".can(read, update)
            }

            uploader = s3Uploader

            beforeCreate {
                    headers, game ->
                println(headers)
            }

            afterCreate { headers, output ->
                println(output)
            }

            beforeGet { headers, query ->
                println(query)
            }

            afterGet {
                    headers, query, games ->
                println(games)
            }

            beforeUpdate { headers, id, patch ->
                println(patch)
            }

            afterUpdate { headers, patch, game ->
                println(game)
            }

            beforeDelete { headers, id ->
                println(id)
            }

            afterDelete { headers, deleteResult ->
                println(deleteResult)
            }
        }

        userModule<User> {
            authorizations {
                guest.can {
                    registered(create, read)
                }

                registered.can {
                    registered(create)
                    "admin".with(update, delete)
                }

                owner(read, update, delete)

                "admin".can {
                    registered(create, read, update, delete)
                    "admin".with(create, read, update, delete)
                }
            }

            uploader = s3Uploader

            facebook(Facebook.appId, Facebook.appSecret)
            apple(Apple.bundleId)
        }
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Game(
    val name: String,
    val platform: Platform?,
    val players: Int?,
    val image: ResourceFile?,
    val secondImage: ResourceFile?): Identifiable(), DateModel {
    override var dateCreated: Date = Date()
    override var dateUpdated: Date = Date()
}

data class Platform(val name: String,
                    val brand: String)

data class User(override var username: String,
                override var password: String?,
                override var facebookId: String?,
                val name: String,
                val surname: String,
                val nickname: String,
                val image: ResourceFile?) : KDoneUser(), DateModel {
    override var dateCreated: Date = Date()
    override var dateUpdated: Date = Date()
}
