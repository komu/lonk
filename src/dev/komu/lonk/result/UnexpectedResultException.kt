package dev.komu.lonk.result

import dev.komu.lonk.LonkException

/**
 * Exception thrown when result from database is unexpected.
 */
public open class UnexpectedResultException(message: String) : LonkException(message)
