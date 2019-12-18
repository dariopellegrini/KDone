package com.dariopellegrini.kdone.model

import kotlin.reflect.KClass

interface Input {
    val input: KClass<*>
}