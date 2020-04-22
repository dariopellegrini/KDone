package com.dariopellegrini.kdone.utils

inline fun <T: Any> guardLet(vararg elements: T?, closure: () -> Nothing): List<T> {
    return if (elements.all { it != null }) {
        elements.filterNotNull()
    } else {
        closure()
    }
}

inline fun <T: Any> ifLet(vararg elements: T?, closure: (List<T>) -> Unit) {
    if (elements.all { it != null }) {
        closure(elements.filterNotNull())
    }
}
 fun randomString(length: Int): String {
     val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
     return (1..length)
         .map { i -> kotlin.random.Random.nextInt(0, charPool.size) }
         .map(charPool::get)
         .joinToString("");
 }