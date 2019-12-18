package com.dariopellegrini.kdone.delegates

import kotlin.reflect.KProperty

class Delegate<T>(var value: T, val action: (() -> Unit)? = null) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
        action?.invoke()
    }
}
