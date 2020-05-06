# KDone

Ktor powered configurator for RESTful API, with authentication.

KDone is an abstraction over Ktor that allows to configure and manage a RESTful backend with models, users, authorization and file upload. KDone makes a plenty use of Kotlin DSL and Ktor APIs, giving developers a declarative and fast way to write a Kotlin REST server from scratch.

## Installation

Add jitpack to your repositories
``` groovy
repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
```
Then add as Kdone dependency
``` groovy
dependencies {
    ...
    implementation 'com.github.dariopellegrini:KDone:v0.5.1'
}
```

## Getting started

KDone's configuration is done using Kotlin DSL, declaring server logic through its models and user logic.

First create a model
```kotlin
data class Game(
    val name: String,
    val platform: Platform?,
    val players: Int?,
    val image: ResourceFile?,
    val secondImage: ResourceFile?): Identifiable()
```

Second create a user model
```kotlin
data class User(override var username: String,
                val nickname: String) : KDoneUser()
```

Then start KDone, declaring a module for your models
```kotlin

startKDone(
        port = 23146,
        mongoURL = "mongodb://localhost:27017/games",
        jwtConfig = JWTConfig("user-key-secret")
    ) {
    
    userModule<User>("users")
    
    module<Game>("games")
  }
    
```

This configuration will open these endpoints for Game model:

- POST http://localhost:23146/games performs creation of a new game
- GET http://localhost:23146/games returns the list of all games
- GET http://localhost:23146/games/:id returns the game with the specified id
- PATCH http://localhost:23146/games/:id performs an update on the game with the specified id
- DELETE http://localhost:23146/games/:id deletes the game with the specified id


This configuration will also open these endpoints for User model:
- POST http://localhost:23146/users/auth/login performs login for user using credentials specified in body, returning a JWT token
- POST http://localhost:23146/users/auth/logout with token performs logout of the current user
- POST http://localhost:23146/users performs a user signup
- GET http://localhost:23146/users/profile/me with token returns the current user
- PATCH http://localhost:23146/users/profile/me with token performs update on the current user
- DELETE http://localhost:23146/users/profile/me with token delete current user

Other functionalities:
- Permissions for users and models
- Support for Apple, Google and Facebook authentications
- Queries
- Geographic coordinates
- Routes actions
- File uploads
- S3 storage support
- Easily extensible with custom routes

## Author

Dario Pellegrini, pellegrini.dario.1303@gmail.com

## License

KDone is available under the MIT license. See the LICENSE file for more info.
