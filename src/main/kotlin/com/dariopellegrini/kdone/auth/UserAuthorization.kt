package com.dariopellegrini.kdone.auth

class UserAuthorization {
    private var userAuthorizationContent: UserAuthorizationContent? = null

    val guestAuthorizationMap = mutableMapOf<String, List<AuthEnum>>(registered.rawValue to listOf(create))
    val registeredAuthorizationMap = mutableMapOf<String, List<AuthEnum>>()
    var ownerAuthorizations = listOf(read, update, delete)

    private val rolesAuthorizationMap = mutableMapOf<String, MutableMap<String, List<AuthEnum>>>()

    private fun guest(closure: UserAuthorizationContent.() -> Unit) {
        if (userAuthorizationContent == null) userAuthorizationContent = UserAuthorizationContent()
        userAuthorizationContent!!.closure()
        guestAuthorizationMap[UserRolesEnum.GUEST.rawValue] = userAuthorizationContent!!.guest
        guestAuthorizationMap[UserRolesEnum.REGISTERED.rawValue] = userAuthorizationContent!!.registered
        userAuthorizationContent!!.roles.forEach { entry ->
            guestAuthorizationMap[entry.key] = entry.value
        }
    }

    private fun registered(closure: UserAuthorizationContent.() -> Unit) {
        if (userAuthorizationContent == null) userAuthorizationContent = UserAuthorizationContent()
        userAuthorizationContent!!.closure()
        registeredAuthorizationMap[UserRolesEnum.GUEST.rawValue] = userAuthorizationContent!!.guest
        registeredAuthorizationMap[UserRolesEnum.REGISTERED.rawValue] = userAuthorizationContent!!.registered
        userAuthorizationContent!!.roles.forEach { entry ->
            registeredAuthorizationMap[entry.key] = entry.value
        }
    }

    fun String.can(closure: UserAuthorizationContent.() -> Unit) {
        if (userAuthorizationContent == null) userAuthorizationContent = UserAuthorizationContent()
        userAuthorizationContent!!.closure()
        if (!rolesAuthorizationMap.containsKey(this)) rolesAuthorizationMap[this] = mutableMapOf()
        rolesAuthorizationMap[this]!![UserRolesEnum.GUEST.rawValue] = userAuthorizationContent!!.guest
        rolesAuthorizationMap[this]!![UserRolesEnum.REGISTERED.rawValue] = userAuthorizationContent!!.registered
        userAuthorizationContent!!.roles.forEach { entry ->
            rolesAuthorizationMap[this]!![entry.key] = entry.value
        }
    }

    fun UserRolesEnum.can(closure: UserAuthorizationContent.() -> Unit) {
        when(this) {
            UserRolesEnum.GUEST -> guest(closure)
            UserRolesEnum.REGISTERED -> registered(closure)
        }
    }

    fun owner(vararg values: AuthEnum) {
        if (userAuthorizationContent == null) userAuthorizationContent = UserAuthorizationContent()
        ownerAuthorizations = listOf(*values)
    }

    fun check(userRole: UserRolesEnum, permission: AuthEnum, destinationRole: String): Boolean {
        return when(userRole) {
            UserRolesEnum.GUEST -> guestAuthorizationMap[destinationRole]?.contains(permission) ?: false
            UserRolesEnum.REGISTERED -> registeredAuthorizationMap[destinationRole]?.contains(permission) ?: false
        }
    }

    fun check(userRole: UserRolesEnum, permission: AuthEnum, destinationRole: UserRolesEnum): Boolean {
        return when(userRole) {
            UserRolesEnum.GUEST -> guestAuthorizationMap[destinationRole.rawValue]?.contains(permission) ?: false
            UserRolesEnum.REGISTERED -> registeredAuthorizationMap[destinationRole.rawValue]?.contains(permission) ?: false
        }
    }

    fun check(userRole: String, permission: AuthEnum, destinationRole: String): Boolean {
        return rolesAuthorizationMap[userRole]?.get(destinationRole)?.contains(permission) ?: false
    }

    fun check(userRole: String, permission: AuthEnum, destinationRole: UserRolesEnum): Boolean {
        return rolesAuthorizationMap[userRole]?.get(destinationRole.rawValue)?.contains(permission) ?: false
    }

    fun checkOwner(permission: AuthEnum): Boolean {
        return ownerAuthorizations.contains(permission)
    }

    // Acceptable roles
    fun acceptableRoles(userRole: UserRolesEnum, permission: AuthEnum): List<String> {
        return when(userRole) {
            UserRolesEnum.GUEST -> guestAuthorizationMap.map {
                if (it.value.contains(permission)) it.key else null
            }.filterNotNull()
            UserRolesEnum.REGISTERED -> registeredAuthorizationMap.map {
                if (it.value.contains(permission)) it.key else null
            }.filterNotNull()
        }
    }

    fun acceptableRoles(userRole: String, permission: AuthEnum): List<String> {
        return rolesAuthorizationMap[userRole]?.map {
            if (it.value.contains(permission)) it.key else null
        }?.filterNotNull() ?: listOf()
    }
}

class UserAuthorizationContent() {
    var guest: List<AuthEnum> = listOf()
    var registered: List<AuthEnum> = listOf()
    var roles: MutableMap<String, List<AuthEnum>> = mutableMapOf()

    fun guest(vararg values: AuthEnum) {
        guest = listOf(*values)
    }
    fun registered(vararg values: AuthEnum) {
        registered = listOf(*values)
    }

    fun String.with(vararg values: AuthEnum) {
        roles[this] = listOf(*values)
    }
}

//authorizations {
//    guest.can {
//        guest(create, read)
//        registered(create, read)
//        "admin".with(create, read, update, delete)
//    }
//
//    registered.can {
//        guest(create, read)
//        registered(create, read)
//        "admin".with(create, read, update, delete)
//    }
//
//    "admin".can {
//        guest(create, read)
//        registered(create, read)
//        "admin".with(create, read, update, delete)
//    }
//}