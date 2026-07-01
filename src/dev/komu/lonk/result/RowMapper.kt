package dev.komu.lonk.result

import java.sql.ResultSet

/**
 * Maps a single row of result-set into an object.
 */
public fun interface RowMapper<T> {
    /**
     * Produces a single value based on current row.
     *
     *
     * The implementation should not call [ResultSet.next] or other methods to move
     * the current position of the [ResultSet], caller is responsible for that.
     */
    public fun mapRow(resultSet: ResultSet): T?

    /**
     * Creates a [ResultSetProcessor] that applies this row-mapper to every row
     * and results a list.
     */
    public fun list(): ResultSetProcessor<List<T>> {
        return ResultSetProcessor { resultSet ->
            val result = mutableListOf<T>()
            while (resultSet.next())
                result.add(mapRow(resultSet) as T)
            result
        }
    }

    /**
     * Creates a [ResultSetProcessor] that expects a single result row from database.
     */
    public fun unique(): ResultSetProcessor<T> {
        return ResultSetProcessor { resultSet ->
            if (!resultSet.next()) throw EmptyResultException()
            val result = mapRow(resultSet)

            if (resultSet.next()) throw NonUniqueResultException()
            result as T
        }
    }

    /**
     * Creates a [ResultSetProcessor] that expects zero or one result row from the database.
     */
    public fun optional(): ResultSetProcessor<T?> {
        return ResultSetProcessor { resultSet ->
            if (!resultSet.next()) {
                null
            } else {
                val result = mapRow(resultSet)

                if (resultSet.next()) throw NonUniqueResultException()
                result
            }
        }
    }
}
