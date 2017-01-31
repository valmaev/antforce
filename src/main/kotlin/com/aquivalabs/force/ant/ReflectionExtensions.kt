package com.aquivalabs.force.ant

import java.lang.reflect.AccessibleObject


internal inline fun <T : AccessibleObject, R> T.asAccessible(body: (T) -> R): R {
    val oldValue = isAccessible
    isAccessible = true
    try {
        return body(this)
    } finally {
        isAccessible = oldValue
    }
}

@Suppress("UNCHECKED_CAST")
internal fun <T> Class<*>.invokeDeclaredMethod(instance: Any?, methodName: String, vararg args: Any?): T =
    getDeclaredMethod(methodName).asAccessible { return it(instance, args) as T }

@Suppress("UNCHECKED_CAST")
internal fun <T> Class<*>.getDeclaredFieldValue(instance: Any?, fieldName: String): T =
    getDeclaredField(fieldName).asAccessible { return it[instance] as T }

@Suppress("UNCHECKED_CAST")
internal fun <T> Class<*>.setDeclaredFieldValue(instance: Any?, fieldName: String, value: T) =
    getDeclaredField(fieldName).asAccessible { it[instance] = value }