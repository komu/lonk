package dev.komu.lonk

import java.sql.SQLException

/**
 * Wraps an [SQLException] originating from database.
 */
public open class DatabaseSQLException : DatabaseException {

    internal constructor(cause: SQLException) : super(cause)

    override val cause: SQLException
        get() = super.cause as SQLException
}
