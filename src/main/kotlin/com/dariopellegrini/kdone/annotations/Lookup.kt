package com.dariopellegrini.kdone.annotations

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Lookup(val parameter: String, val collectionName: String)