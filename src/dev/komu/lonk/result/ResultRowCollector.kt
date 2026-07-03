package dev.komu.lonk.result

import java.sql.ResultSet

/**
 * Callback for processing a whole [ResultSet].
 */
public interface ResultRowCollector<T> {

    /**
     * Called to process rows in the result.
     *
     * @return true if this collector is ready to process more rows, otherwise false
     */
    public fun accumulate(row: ResultRow): Boolean

    /** Build the final result from the collected state. */
    public fun finish(): T

    /**
     * Optional hint for the maximum number of rows this collector will ever need,
     * allowing the backend to avoid computing or fetching rows beyond it. `null` means
     * no known bound.
     */
    public val rowLimitHint: Int?
        get() = null
}
