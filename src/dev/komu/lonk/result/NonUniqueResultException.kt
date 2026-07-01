package dev.komu.lonk.result

/**
 * Exception thrown when expecting a unique result from a call, but more then one row
 * of results was returned by the database.
 * 
 * @see Database.findUnique
 * @see Database.findOptional
 * @see Database.findUniqueOrNull
 * @see EmptyResultException
 */
public open class NonUniqueResultException : UnexpectedResultException {
    internal constructor() : super("Expected unique result but received more than one row")

    protected constructor(message: String) : super(message)
}
