package dev.komu.lonk.result

import java.sql.ResultSet

/**
 * Callback for processing a whole [ResultSet].
 */
public fun interface ResultSetProcessor<T> {
    public fun process(resultSet: ResultSet): T
}
