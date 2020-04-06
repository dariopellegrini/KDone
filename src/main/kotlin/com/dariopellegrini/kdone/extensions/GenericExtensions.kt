package com.dariopellegrini.kdone.extensions

import com.dariopellegrini.kdone.exceptions.ConstructionException
import kotlin.reflect.KParameter
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

inline fun <reified I: Any, reified O: Any>I.map(): O {
    val ctor = O::class.primaryConstructor ?: throw ConstructionException("Primary constructor for ${O::class} is missing")
    val propertiesByName = I::class.memberProperties.associateBy { it.name }
    return ctor. callBy(ctor.parameters.filter {
        propertiesByName.containsKey(it.name) // Probably default parameter
    }.associateWith {
        propertiesByName[it.name]?.get(this@map)
    })
}

inline fun <reified I: Any, reified O: Any>I.mapUsing(closure: (KParameter, I) -> Any): O {
    val ctor = O::class.primaryConstructor ?: throw ConstructionException("Primary constructor for ${O::class} is missing")
    val propertiesByName = I::class.memberProperties.associateBy { it.name }
    return ctor. callBy(ctor.parameters.filter {
        propertiesByName.containsKey(it.name) // Probably default parameter
    }.associateWith {
        when (val value = closure(it, this)) {
            is Unit -> propertiesByName[it.name]?.get(this@mapUsing)
            else -> value
        }
    })
}