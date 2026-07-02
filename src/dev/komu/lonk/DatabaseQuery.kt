package dev.komu.lonk

import kotlin.time.Duration

internal class DatabaseQuery(
    val sql: String,
    val arguments: List<Any?>,
    val timeout: Duration?,
)
