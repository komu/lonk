package dev.komu.lonk

import org.intellij.lang.annotations.Language

/**
 * Represents an SQL query along all of its arguments.
 */
public data class SqlQuery(
    @param:Language("SQL") val sql: String,
    val arguments: List<*>,
) {

    override fun toString(): String {
        val sb = StringBuilder(10 + sql.length + 10 * arguments.size)

        sb.append(sql)

        arguments.joinTo(sb, separator = ", ", prefix = " [", postfix = "]") { it.toString() }

        return sb.toString()
    }
}
