package com.dariopellegrini.kdone.auth

enum class UserRolesEnum(val rawValue: String) {
    GUEST("GUEST"),
    REGISTERED("REGISTERED")
}

val guest get() = UserRolesEnum.GUEST
val registered get() = UserRolesEnum.REGISTERED
