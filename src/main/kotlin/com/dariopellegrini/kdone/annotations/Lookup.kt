package com.dariopellegrini.kdone.annotations

import org.bson.conversions.Bson

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Lookup(val parameter: String,
                        val collectionName: String,
                        val foreignParameter: String = "_id")