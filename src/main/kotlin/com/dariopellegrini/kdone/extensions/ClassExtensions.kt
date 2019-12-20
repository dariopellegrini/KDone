package com.dariopellegrini.kdone.extensions

import com.dariopellegrini.kdone.model.DateModel
import com.dariopellegrini.kdone.model.GeoLocation

val <T>Class<T>.geoIndexJson: List<String>?
    get() {
        return Class.forName(this.name).declaredFields
            .filter { GeoLocation::class.java.isAssignableFrom(it.type) }
            .map { field ->
            """
                { ${field.name}:"2dsphere" }
                """.trimIndent()
        }
    }

val <T>Class<T>.isDateModel: Boolean
    get() {
        return DateModel::class.java.isAssignableFrom(this)
    }