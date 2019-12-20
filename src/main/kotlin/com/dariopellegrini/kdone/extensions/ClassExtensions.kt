package com.s4win.vertirest.extensions

import com.s4win.vertirest.date.DateModel
import com.s4win.vertirest.models.GeoLocation
import io.vertx.core.json.JsonObject

val <T>Class<T>.geoIndexJson: JsonObject?
    get() {
        var jsonObject: JsonObject? = null
        Class.forName(this.name).declaredFields.forEach { field ->
            if (GeoLocation::class.java.isAssignableFrom(field.type)) {
                if (jsonObject == null) {
                    jsonObject = JsonObject()
                }
                jsonObject?.put(field.name, "2dsphere")

            }
        }
        return jsonObject
    }

val <T>Class<T>.isDateModel: Boolean
    get() {
        return DateModel::class.java.isAssignableFrom(this)
    }