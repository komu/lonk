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
    public fun mapRow(resultSet: ResultRow): T?

    /**
     * Creates a [ResultAggregator] that applies this row-mapper to every row
     * and results a list.
     */
    public fun list(): ResultAggregator<List<T>> {
        return object : ResultAggregator<List<T>> {
            private val result = mutableListOf<T>()

            override fun process(row: ResultRow) {
                result.add(mapRow(row) as T)
            }

            override fun build() =
                result
        }
    }

    /**
     * Creates a [ResultAggregator] that expects a single result row from database.
     */
    public fun unique(): ResultAggregator<T> {
        return object : ResultAggregator<T> {
            private var result: T? = null
            private var got: Boolean = false

            override fun process(row: ResultRow) {
                if (got) throw NonUniqueResultException()
                result = mapRow(row) as T
                got = true
            }

            @Suppress("UNCHECKED_CAST")
            override fun build() = if (got) result as T else throw EmptyResultException()
        }
    }

    /**
     * Creates a [ResultAggregator] that expects zero or one result rows from the database.
     */
    public fun optional(): ResultAggregator<T?> {
        return object : ResultAggregator<T?> {
            private var result: T? = null
            private var got: Boolean = false

            override fun process(row: ResultRow) {
                if (got) throw NonUniqueResultException()
                result = mapRow(row) as T
                got = true
            }

            @Suppress("UNCHECKED_CAST")
            override fun build() = result
        }
    }
}
