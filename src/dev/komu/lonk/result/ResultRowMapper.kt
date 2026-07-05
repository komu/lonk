package dev.komu.lonk.result

/** A function that maps a [ResultRow] into a value of type [T]. */
public typealias ResultRowMapper<T> = (ResultRow) -> T
