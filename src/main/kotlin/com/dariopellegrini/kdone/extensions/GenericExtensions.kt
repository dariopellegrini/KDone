package com.dariopellegrini.kdone.extensions

import com.dariopellegrini.kdone.exceptions.ConstructionException
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

fun hello() {
    val input = PersonInput("D", "P", "https://images.com/1.jpg")
    val person: Person = input.remodelWith { parameter, value ->
        when(parameter) {
            PersonInput::image -> Image(value.image, "image/jpg")
            else -> Unit
        }
    }
    println(person)
}

data class Person(val id: String = "id1", val name: String, val surname: String, val image: Image)
data class PersonInput(val name: String, val surname: String, val image: String)
data class Image(val src: String, val mimeType: String)

inline fun <reified I: Any, reified O: Any>I.remodel(): O {
    val constructor = O::class.primaryConstructor ?: throw ConstructionException("Primary constructor for ${O::class} is missing")
    val propertiesByName = I::class.memberProperties.associateBy { it.name }
    return constructor. callBy(constructor.parameters.filter {
        propertiesByName.containsKey(it.name) // Probably default parameter
    }.associateWith {
        propertiesByName[it.name]?.get(this@remodel)
    })
}

inline fun <reified I: Any, reified O: Any>I.remodelWith(closure: (KProperty1<I, *>, I) -> Any): O {
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
            is Unit -> property.get(this@remodelWith)
            else -> value
        }
    })
}