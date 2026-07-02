package dev.komu.lonk

/**
 * Base class for all of Lonk's exceptions.ba
 */
public open class LonkException(
    message: String? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
