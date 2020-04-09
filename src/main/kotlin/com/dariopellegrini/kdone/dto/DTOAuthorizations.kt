package com.dariopellegrini.kdone.dto

import kotlin.reflect.KProperty1

class DTOAuthorizations<T: Any> {
    var readDto: DtoRule<T, *>? = null

    inline fun <reified R: Any>read() {
        readDto = DtoRule(R::class)
    }

    inline fun <reified R: Any>read(noinline closure: (KProperty1<T, *>, T) -> R?) {
        readDto = DtoRule(R::class, closure)
    }

    inline fun <reified R: Any>read(noinline init: (T) -> R) {
        readDto = DtoRule(R::class, init = init)
    }
}