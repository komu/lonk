package dev.komu.lonk.instantiation

import dev.komu.lonk.DatabaseException

/**
 * Exception thrown when there is a problem with instantiation or conversion.
 */
internal class InstantiationFailureException : DatabaseException {
    constructor(message: String) : super(message)
}
