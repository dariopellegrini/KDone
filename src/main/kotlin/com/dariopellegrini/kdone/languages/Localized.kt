package com.dariopellegrini.kdone.languages

import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure

class Localized<T: Any>: HashMap<String, T>() {
    fun localize(countryCode: String) = get(countryCode)
}

@Suppress("UNCHECKED_CAST")
fun Any.localize(countryCode: String, defaultCountryCode: String): Map<String, Any?> {
    val klass = this::class
    val values = mutableMapOf<String, Any?>()
    klass.memberProperties.forEach {
        member ->
        if (member.returnType.jvmErasure.isSubclassOf(Localized::class)) {
            val local = ((member as? KProperty1<Any, *>)?.get(this) as? Localized<*>)
            values[member.name] = local?.localize(countryCode) ?: local?.localize(defaultCountryCode) ?:
                    (member as KProperty1<Any, *>).get(this)
        } else {
            values[member.name] = (member as KProperty1<Any, *>).get(this)
        }
    }
    return values
}
