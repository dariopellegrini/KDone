package com.dariopellegrini.kdone.interfaces

interface RouteActions {
    suspend fun beforeCreate(input: Map<String, Any>) {}
}