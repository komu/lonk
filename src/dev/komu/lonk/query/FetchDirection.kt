package dev.komu.lonk.query

import java.sql.ResultSet

/**
 * Represents fetch direction that can be given as a hint to [SqlQuery.setFetchDirection].
 * 
 * @see java.sql.PreparedStatement.setFetchDirection
 */
public enum class FetchDirection(internal val jdbcCode: Int) {
    FORWARD(ResultSet.FETCH_FORWARD),
    REVERSE(ResultSet.FETCH_REVERSE),
    UNKNOWN(ResultSet.FETCH_UNKNOWN)
}
