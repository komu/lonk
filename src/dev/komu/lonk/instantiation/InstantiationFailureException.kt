package dev.komu.lonk.instantiation

import dev.komu.lonk.LonkException

/**
 * Exception thrown when there is a problem with instantiation or conversion.
 */
internal class InstantiationFailureException : LonkException {
    constructor(message: String) : super(message)
}
