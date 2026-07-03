package dev.komu.lonk

/**
 * Base class for all of Lonk's exceptions.
 */
public open class LonkException internal constructor(
    message: String? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/**
 * Exception thrown when expecting a unique result from a call but got no results.
 */
public class EmptyResultException : NonUniqueResultException("Expected unique result, but got no rows")

/**
 * Exception thrown when expecting a unique result from a call, but more than one row
 * of results was returned by the database.
 */
public open class NonUniqueResultException : UnexpectedResultException {
    internal constructor() : super("Expected unique result but received more than one row")

    protected constructor(message: String) : super(message)
}

/** Exception thrown when a result from the database is unexpected. */
public open class UnexpectedResultException internal constructor(message: String) : LonkException(message)

/** Exception thrown when there is a problem with instantiation or conversion. */
public class InstantiationFailureException internal constructor(message: String) : LonkException(message)
