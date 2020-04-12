package com.dariopellegrini.kdone.dto

import kotlin.reflect.KProperty1

class DTOAuthorizations<T: Any> {
    var readDto: DtoOutputRule<T, *>? = null
    var createDto: DtoInputRule<*, T>? = null
    var updateDto: DtoInputRule<*, T>? = null

    // Read
    inline fun <reified R: Any>read() {
        readDto = DtoOutputRule(R::class)
    }

    inline fun <reified R: Any>read(noinline closure: (KProperty1<T, *>, T) -> R?) {
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
}