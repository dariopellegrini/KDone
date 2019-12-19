# KDone

Ktor powered configurator for RESTful API with authentication.

KDone is an abstraction over Ktor that allows to configure and manage a RESTful backend with models, users, authorization and file upload. KDone makes a plenty use of Kotlin DSL and Ktor APIs, giving developers a declarative and fast way to write a Kotlin REST server from scratch.

## Installation

Import KDone as a Gradle dependency
``` groovy
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
Then add as dependency to your app/build.gradle
``` groovy
dependencies {
    ...
    implementation 'com.github.dariopellegrini:KDone:v0.1'
}
```

## Getting started

KDone's configuration is done using Kotlin DSL, declaring server logic through its models and user logic.

```kotlin

startKDone(
        port = System.getenv("PORT")?.toInt() ?: 23146,
        mongoURL = "mongodb://localhost:27017/games",
        jwtConfig = JWTConfig("secret")
    ) {
        val s3Uploader = S3Uploader(
            baseFolder = "kdone",
            baseURL = "https://image.provider.com",
            bucketName = "kdonebucket",
            accessKey = "ACCESSKEY",
            secretKey = "SECRETKEY",
            serviceEndpoint = "https://ams3.digitaloceanspaces.com",
            signingRegion = "ams3"
        )

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
            
            apple(Apple.bundleId)
            facebook(Facebook.appId, Facebook.appSecret)
        }

        module<Game>("games") {
            authorizations {
                guest()
                registered(create, read)
                owner(read, update, delete)
                "admin".can(create, read, update, delete)
                "officer".can(read, update)
            }

            uploader = s3Uploader
        }
    }
    
```

## Author

Dario Pellegrini, pellegrini.dario.1303@gmail.com

## License

KDone is available under the MIT license. See the LICENSE file for more info.
