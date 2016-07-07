package com.aquivalabs.force.ant

import java.lang.reflect.Field
import java.util.*


internal inline fun <T> Field.accessible(body: (Field) -> T): T {
    val oldValue = isAccessible
    isAccessible = true
    try {
        return body(this)
    } finally {
        isAccessible = oldValue
    }
}

internal fun Double.format(digits: Int) = String.format(Locale.US, "%.${digits}f", this)