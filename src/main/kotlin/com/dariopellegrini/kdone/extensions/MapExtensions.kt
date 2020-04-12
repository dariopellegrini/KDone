package com.dariopellegrini.kdone.extensions

import com.dariopellegrini.kdone.exceptions.MapCheckException
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure

inline fun <reified T: Any> Map<String, Any>.checkWithType() {
    val kClass = T::class
    val parametersMap = kClass.memberProperties.associateBy { it.name }
    this.forEach {
        entry ->
        val key = entry.key
        val value = entry.value
        val property = parametersMap[key] ?: throw MapCheckException("Parameter $key is not present $kClass")

        if (property.returnType.jvmErasure != value::class) throw MapCheckException("Parameter $key is not compatible with $kClass")
    }
}