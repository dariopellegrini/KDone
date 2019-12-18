package com.dariopellegrini.kdone

import com.dariopellegrini.kdone.auth.*
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
        jwtConfig = JWTConfig("secret")) {

        val s3Uploader = S3Uploader(
            baseFolder = "kdone",
            baseURL = "https://image.provider.com",
            bucketName = "kdonebucket",
            accessKey = "ACCESSKEY",
            secretKey = "SECRETKEY",
            serviceEndpoint = "https://ams3.digitaloceanspaces.com",
            signingRegion = "ams3"
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
                override var password: String,
                val name: String,
                val surname: String,
                val nickname: String,
                val image: ResourceFile?) : KDoneUser(), DateModel {
    override var dateCreated: Date = Date()
    override var dateUpdated: Date = Date()
}
