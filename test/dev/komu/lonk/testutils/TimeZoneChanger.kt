package dev.komu.lonk.testutils

import java.util.*

internal inline fun withUTCTimeZone(block: () -> Unit) {
    withTimeZone("UTC", block)
}

internal inline fun withTimeZone(name: String, block: () -> Unit) {
    val old = TimeZone.getDefault()
    try {
        TimeZone.setDefault(TimeZone.getTimeZone(name))
        block()
    } finally {
        TimeZone.setDefault(old)
    }
}
