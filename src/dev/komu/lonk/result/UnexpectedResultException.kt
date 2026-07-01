package dev.komu.lonk.result

import dev.komu.lonk.DatabaseException

/**
 * Exception thrown when result from database is unexpected.
 */
public open class UnexpectedResultException(message: String) : DatabaseException(message)
