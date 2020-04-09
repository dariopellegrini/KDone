package com.dariopellegrini.kdone.dto

import auth.mongoId
import com.dariopellegrini.kdone.auth.UserAuth
import com.dariopellegrini.kdone.model.Identifiable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

class DTOConfiguration<T: Any> {
    var readDto: DtoRule<T, *>? = null
//    var createDto: DtoCreateRule<*, T>? = null

    var guestAuthorizations: DTOAuthorizations<T>? = null
    var registeredAuthorizations: DTOAuthorizations<T>? = null
    var ownerAuthorizations: DTOAuthorizations<T>? = null
    var authorizationsMap: MutableMap<String, DTOAuthorizations<T>>? = null

    // Read
    inline fun <reified R: Any>read() {
        readDto = DtoRule(R::class)
    }

    inline fun <reified R: Any>read(noinline closure: (KProperty1<T, *>, T) -> R?) {
        readDto = DtoRule(R::class, closure)
    }

    inline fun <reified R: Any>read(noinline init: (T) -> R) {
        readDto = DtoRule(R::class, init = init)
    }

    // Create
//    inline fun <reified R: Any>create() {
//        createDto = DtoCreateRule(R::class)
//    }
//
//    inline fun <reified R: Any>create(noinline closure: (KProperty1<R, *>, R) -> T?) {
//        createDto = DtoCreateRule(R::class, closure)
//    }
//
//    inline fun <reified R: Any>create(noinline init: (R) -> T) {
//        createDto = DtoCreateRule(R::class, init)
//    }

    fun guest(closure: DTOAuthorizations<T>.() -> Unit) {
        guestAuthorizations = DTOAuthorizations()
        guestAuthorizations?.closure()
    }

    fun registered(closure: DTOAuthorizations<T>.() -> Unit) {
        registeredAuthorizations = DTOAuthorizations()
        registeredAuthorizations?.closure()
    }

    fun owner(closure: DTOAuthorizations<T>.() -> Unit) {
        ownerAuthorizations = DTOAuthorizations()
        ownerAuthorizations?.closure()
    }

    fun String.dto(closure: DTOAuthorizations<T>.() -> Unit) {
        if (authorizationsMap == null) {
            authorizationsMap = mutableMapOf()
        }
        authorizationsMap?.set(this, DTOAuthorizations<T>().apply(closure))
    }

    fun readDTO(userAuth: UserAuth?, element: T): DtoRule<T, *>? {
        val readDto =
            if (userAuth?.role != null && authorizationsMap?.containsKey(userAuth.role) == true)
                authorizationsMap?.get(userAuth.role)?.readDto
            else if (userAuth != null && element is Identifiable && element.owner == userAuth.userId.mongoId<T>())
                ownerAuthorizations?.readDto
            else if (userAuth != null)
                registeredAuthorizations?.readDto
            else
                guestAuthorizations?.readDto
        return readDto ?: this.readDto
    }
}
class DtoRule<I: Any, O: Any>(val kClass: KClass<O>,
                 val closure: ((KProperty1<I, *>, I) -> Any?)? = null,
                 val init: ((I) -> O)? = null)

class DtoCreateRule<I: Any, O: Any>(val kClass: KClass<I>, val init: ((I) -> O)? = null)