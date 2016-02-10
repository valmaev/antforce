package com.newmarket.force.ant

import java.lang.reflect.Field


internal inline fun <T> Field.accessible(body: (Field) -> T): T {
    val oldValue = isAccessible
    isAccessible = true
    try {
        return body(this)
    } finally {
        isAccessible = oldValue
    }
}