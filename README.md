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
- Roles
- Support for Apple, Google and Facebook authentications
- One-time password
- Queries
- Geographic coordinates
- Routes actions
- DTO
- File uploads
- S3 storage support
- Easily extensible with custom routes

### Documentation
See [KDone website](https://dariopellegrini.github.io/kdone-website) or [Wiki page](https://github.com/dariopellegrini/KDone/wiki) for more details.

## Author

Dario Pellegrini, pellegrini.dario.1303@gmail.com

## License

KDone is available under the MIT license. See the LICENSE file for more info.

<br>

<a href="https://www.paypal.com/donate/?business=R8TE7AE9D5MRC&no_recurring=0&currency_code=EUR">
  <img src="https://raw.githubusercontent.com/stefan-niedermann/paypal-donate-button/master/paypal-donate-button.png" alt="Donate with PayPal" width=200px />
</a>

<!---
[!["Buy Me A Coffee"](https://www.buymeacoffee.com/assets/img/custom_images/orange_img.png)](https://www.buymeacoffee.com/dpellegrini)
-->
