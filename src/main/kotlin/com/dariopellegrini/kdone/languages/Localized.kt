package com.dariopellegrini.kdone.languages

import com.dariopellegrini.kdone.extensions.LocalizedSerializer
import com.dariopellegrini.kdone.extensions.configureForKDone
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.convertValue
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure

class Localized<T: Any>: HashMap<String, T>() {
    fun localize(countryCode: String) = get(countryCode)
}

@Suppress("UNCHECKED_CAST")
fun Any.localize(countryCode: String?, defaultCountryCode: String = "en"): Any {
    val objectMapper = ObjectMapper().configureForKDone()
    val localizedModule = SimpleModule().apply {
        addSerializer(Localized::class.java, LocalizedSerializer(countryCode ?: defaultCountryCode))
    }
    objectMapper.registerModule(localizedModule)
    return objectMapper.convertValue(this)
//    val klass = this::class
//    val values = mutableMapOf<String, Any?>()
//    klass.memberProperties.forEach {
//        member ->
//        if (member.returnType.jvmErasure.isSubclassOf(Localized::class)) {
//            val local = ((member as? KProperty1<Any, *>)?.get(this) as? Localized<*>)
//            values[member.name] = local?.localize(countryCode) ?: local?.localize(defaultCountryCode) ?:
//                    if (local?.keys?.firstOrNull() != null) local.localize(local.keys.first()) else
//                    null
//        } else {
//            values[member.name] = (member as KProperty1<Any, *>).get(this)
//        }
//    }
//    return values
}
