package com.dariopellegrini.kdone.languages

import com.dariopellegrini.kdone.extensions.LocalizationSerializer
import com.dariopellegrini.kdone.extensions.LocalizedSerializer
import com.dariopellegrini.kdone.extensions.configureForKDone
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.convertValue

@Deprecated("Use Localization instead")
class Localized<T: Any>: HashMap<String, T>() {
    fun localize(countryCode: String) = get(countryCode)
}

class Localization<T: Any>(val elements: List<LocalizationElement<T>>) {
    fun get(key: String): T? {
        return elements.firstOrNull { it.key == key }?.value
    }
}

data class LocalizationElement<T: Any>(val key: String, val value: T)

@Suppress("UNCHECKED_CAST")
fun Any.localize(countryCode: String?, defaultCountryCode: String = "en"): Any {
    val objectMapper = ObjectMapper().configureForKDone()
    val localizedModule = SimpleModule().apply {
        addSerializer(Localized::class.java, LocalizedSerializer(countryCode ?: defaultCountryCode))
    }
    val localizationModule = SimpleModule().apply {
        addSerializer(Localization::class.java, LocalizationSerializer(countryCode ?: defaultCountryCode))
    }
    objectMapper.registerModule(localizedModule)
    objectMapper.registerModule(localizationModule)
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
