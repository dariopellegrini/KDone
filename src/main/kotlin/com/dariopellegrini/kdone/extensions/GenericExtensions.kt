package com.dariopellegrini.kdone.extensions

import com.dariopellegrini.kdone.exceptions.ConstructionException
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

inline fun <reified I: Any, reified O: Any>I.transfer(): O {
    val constructor = O::class.primaryConstructor ?: throw ConstructionException("Primary constructor for ${O::class} is missing")
    val propertiesByName = I::class.memberProperties.associateBy { it.name }
    return constructor. callBy(constructor.parameters.filter {
        propertiesByName.containsKey(it.name) // Probably default parameter
    }.associateWith {
        propertiesByName[it.name]?.get(this@transfer)
    })
}

inline fun <reified I: Any, reified O: Any>I.transfer(closure: (KProperty1<I, *>, I) -> Any?): O {
    val constructor = O::class.primaryConstructor ?: throw ConstructionException("Primary constructor for ${O::class} is missing")
    val propertiesByName = I::class.memberProperties.associateBy { it.name }
    propertiesByName.map {
        it.value
    }
    return constructor.callBy(constructor.parameters.filter {
        propertiesByName.containsKey(it.name) // Probably default parameter
    }.associateWith {
        val property = propertiesByName[it.name] ?: return@associateWith null
        when (val value = closure(property, this)) {
            is Unit -> property.get(this@transfer)
            else -> value
        }
    })
}

fun <I: Any, O: Any>I.transfer(inputClass: KClass<I>, outputClass: KClass<O>): O {
    val constructor = outputClass.primaryConstructor ?: throw ConstructionException("Primary constructor for $outputClass is missing")
    val propertiesByName = inputClass.memberProperties.associateBy { it.name }
    return constructor. callBy(constructor.parameters.filter {
        propertiesByName.containsKey(it.name) // Probably default parameter
    }.associateWith {
        propertiesByName[it.name]?.get(this@transfer)
    })
}

fun <I: Any, O: Any>I.transfer(inputClass: KClass<I>, outputClass: KClass<O>, closure: (KProperty1<I, *>, I) -> Any?): O {
    val constructor = outputClass.primaryConstructor ?: throw ConstructionException("Primary constructor for $outputClass is missing")
    val propertiesByName = inputClass.memberProperties.associateBy { it.name }
    propertiesByName.map {
        it.value
    }
    return constructor.callBy(constructor.parameters.filter {
        propertiesByName.containsKey(it.name) // Probably default parameter
    }.associateWith {
        val property = propertiesByName[it.name] ?: return@associateWith null
        when (val value = closure(property, this)) {
            is Unit -> property.get(this@transfer)
            else -> value
        }
    })
}
