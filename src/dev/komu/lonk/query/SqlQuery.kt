package dev.komu.lonk.query

import org.intellij.lang.annotations.Language
import kotlin.time.Duration

/**
 * Represents an SQL query along all of its arguments.
 */
public data class SqlQuery(
    @param:Language("SQL") val sql: String,
    val arguments: List<*>,
    val fetchSize: Int? = null,
    val fetchDirection: FetchDirection? = null,
    val timeout: Duration? = null,
) {

    init {
        require(fetchSize == null || fetchSize >= 0) { "Illegal fetch size $fetchSize. Fetch size must be null or >= 0" }
        require(timeout == null || timeout >= Duration.ZERO) { "Negative timeout: $timeout" }
    }

    override fun toString(): String {
        val sb = StringBuilder(10 + sql.length + 10 * arguments.size)

        sb.append(sql)

        arguments.joinTo(sb, separator = ", ", prefix = " [", postfix = "]") { it.toString() }

        return sb.toString()
    }
}

/**
 * Creates a new [SqlQuery] consisting of given SQL statement and arguments.
 * '?' characters act as placeholders for arguments in the query.
 */
public fun query(@Language("SQL") sql: String, vararg args: Any?): SqlQuery =
    SqlQuery(sql, args.asList())

/**
 * @see .query
 */
public fun query(@Language("SQL") sql: String, args: List<*>): SqlQuery =
    SqlQuery(sql, args)
