package com.dariopellegrini.kdone.auth

enum class AuthEnum {
    CREATE,
    READ,
    UPDATE,
    DELETE
}

val create get() = AuthEnum.CREATE
val read get() = AuthEnum.READ
val update get() = AuthEnum.UPDATE
val delete get() = AuthEnum.DELETE
