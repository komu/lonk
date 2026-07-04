package dev.komu.lonk

/**
 * Base class for all of Lonk's exceptions.
 */
public open class LonkException internal constructor(
    message: String? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/**
 * Exception thrown when a call expecting a unique result received zero rows, or more than one row,
 * instead of exactly one.
 */
public class UnexpectedResultException internal constructor(message: String) : LonkException(message)

/** Exception thrown when there is a problem with instantiation or conversion. */
public class InstantiationFailureException internal constructor(message: String) : LonkException(message)
