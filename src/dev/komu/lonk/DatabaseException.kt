package dev.komu.lonk

/**
 * Base class for all of Lonk's exceptions.ba
 */
public open class DatabaseException(
    message: String? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {

    internal constructor(cause: Throwable) : this(message = null, cause = cause)
}
