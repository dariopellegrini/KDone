package com.dariopellegrini.kdone.auth

class Authorization {
    var guest: List<AuthEnum> = listOf()
    var registered: List<AuthEnum> = listOf()
    var owner: List<AuthEnum> = listOf()
    var roles: MutableMap<String, List<AuthEnum>> = mutableMapOf()

    fun guest(vararg values: AuthEnum) {
        guest = listOf(*values)
    }
    fun registered(vararg values: AuthEnum) {
        registered = listOf(*values)
    }
    fun owner(vararg values: AuthEnum) {
        owner = listOf(*values)
    }

    fun String.can(vararg values: AuthEnum) {
        roles.put(this, listOf(*values))
    }
}