package com.dariopellegrini.kdone.dto

import auth.mongoId
import com.dariopellegrini.kdone.auth.UserAuth
import com.dariopellegrini.kdone.exceptions.ConstructionException
import com.dariopellegrini.kdone.model.Identifiable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

class DTOConfiguration<T: Any> {
    var readDto: DtoOutputRule<T, *>? = null
    var createDto: DtoInputRule<*, T>? = null
    var updateDto: DtoInputRule<*, T>? = null

    var guestAuthorizations: DTOAuthorizations<T>? = null
    var registeredAuthorizations: DTOAuthorizations<T>? = null
    var ownerAuthorizations: DTOAuthorizations<T>? = null
    var authorizationsMap: MutableMap<String, DTOAuthorizations<T>>? = null

    // Read
    inline fun <reified R: Any>read() {
        readDto = DtoOutputRule(R::class)
    }

    private inline fun <reified R: Any>read(noinline closure: (KProperty1<T, *>, T) -> R?) {
        readDto = DtoOutputRule(R::class, closure)
    }

    inline fun <reified R: Any>read(noinline init: (T) -> R) {
        readDto = DtoOutputRule(R::class, init = init)
    }

    // Create
    inline fun <reified R: Any>create() {
        createDto = DtoInputRule(R::class)
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified R: Any>create(noinline init: (R) -> T) {
        createDto = DtoInputRule(R::class, init = (init as (Any) -> T))
    }

    // Update
    inline fun <reified R: Any>update() {
        updateDto = DtoInputRule(R::class)
    }

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

    fun readDTO(userAuth: UserAuth?, element: T): DtoOutputRule<T, *>? {
        val readDto =
            if (userAuth?.role != null)
                if (authorizationsMap?.containsKey(userAuth.role) == true) authorizationsMap?.get(userAuth.role)?.readDto
                else if (element is Identifiable && element.owner == userAuth.userId.mongoId<T>()) ownerAuthorizations?.readDto
                else null
            else if (userAuth != null)
                if (element is Identifiable && element.owner == userAuth.userId.mongoId<T>()) ownerAuthorizations?.readDto
                else registeredAuthorizations?.readDto
            else
                guestAuthorizations?.readDto
        return readDto ?: this.readDto
    }

    fun createDTO(userAuth: UserAuth?): DtoInputRule<*, T>? {
        val createDto =
            if (userAuth?.role != null)
                if (authorizationsMap?.containsKey(userAuth.role) == true) authorizationsMap?.get(userAuth.role)?.createDto
                else null
            else if (userAuth != null)
                registeredAuthorizations?.createDto
            else
                guestAuthorizations?.createDto
        return createDto ?: this.createDto
    }

    fun updateDTO(userAuth: UserAuth?, element: T): DtoInputRule<*, T>? {
        val updateDto =
            if (userAuth?.role != null)
                if (authorizationsMap?.containsKey(userAuth.role) == true) {
                    authorizationsMap?.get(userAuth.role)?.updateDto
                }
                else {
                    if (element is Identifiable && element.owner == userAuth.userId.mongoId<T>()) ownerAuthorizations?.updateDto
                    else null
                }
            else if (userAuth != null) {
                if (element is Identifiable && element.owner == userAuth.userId.mongoId<T>()) ownerAuthorizations?.updateDto
                else registeredAuthorizations?.updateDto
            }
            else
                guestAuthorizations?.updateDto
        return updateDto ?: this.updateDto
    }
}
class DtoOutputRule<I: Any, O: Any>(val kClass: KClass<O>,
                                    val closure: ((KProperty1<I, *>, I) -> Any?)? = null,
                                    val init: ((I) -> O)? = null)

class DtoInputRule<I: Any, O: Any>(val kClass: KClass<I>,
                                   val closure: ((KProperty1<I, *>, I) -> Any?)? = null,
                                   val init: ((Any) -> O)? = null)

@Suppress("UNCHECKED_CAST")
fun <I: Any, O: Any>I.transferAny(inputClass: KClass<*>, outputClass: KClass<O>): O {
    val constructor = outputClass.primaryConstructor ?: throw ConstructionException("Primary constructor for $outputClass is missing")
    val propertiesByName = (inputClass as KClass<I>).memberProperties.associateBy { it.name }
    return constructor. callBy(constructor.parameters.filter {
        propertiesByName.containsKey(it.name) // Probably default parameter
    }.associateWith {
        propertiesByName[it.name]?.get(this@transferAny)
    })
}