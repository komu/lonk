package dev.komu.lonk.query

/**
 * A very simple builder that can be used to build queries dynamically.
 * Operates on a slightly higher level than StringBuilder in that it tracks
 * the query-variables and can be used as a building block for higher level
 * abstractions.
 */
public class QueryBuilder {
    /** The SQL query gathered so far  */
    private val query = StringBuilder(100)

    /** The arguments gathered so far  */
    private val arguments = mutableListOf<Any?>()

    /**
     * Constructs a new empty QueryBuilder.
     */
    public constructor()

    /**
     * Constructs a QueryBuilder with given initial SQL-fragment.
     */
    public constructor(sql: String) {
        append(sql)
    }

    /**
     * Constructs a QueryBuilder with given initial SQL-fragment and arguments.
     */
    public constructor(sql: String, vararg arguments: Any?) {
        append(sql, *arguments)
    }

    /**
     * Appends given fragment and arguments to this query.
     */
    @IgnorableReturnValue
    public fun append(sql: String, vararg args: Any?): QueryBuilder {
        return append(sql, args.asList())
    }

    /**
     * Appends given fragment and arguments to this query.
     */
    /**
     * Appends given fragment to this query.
     */
    @IgnorableReturnValue
    public fun append(sql: String, args: Collection<*> = listOf<Any?>()): QueryBuilder {
        query.append(sql)
        addArguments(args)
        return this
    }

    /**
     * Appends given query and its arguments to this query.
     */
    @IgnorableReturnValue
    public fun append(query: SqlQuery): QueryBuilder {
        this.query.append(query.sql)
        arguments.addAll(query.arguments)
        return this
    }

    /**
     * Adds a given amount of comma-separated place-holders. The amount must be at last 1.
     */
    @IgnorableReturnValue
    public fun appendPlaceholders(count: Int): QueryBuilder {
        require(count > 0) { "count must be positive, but was: $count" }

        query.append('?')
        for (i in 1..<count) query.append(",?")

        return this
    }

    /**
     * Adds placeholders for all elements of collection and then adds then values
     * of collection itself.
     */
    @IgnorableReturnValue
    public fun appendPlaceholders(args: Collection<*>): QueryBuilder {
        appendPlaceholders(args.size)
        addArguments(args)

        return this
    }

    public val isEmpty: Boolean
        /**
         * Is the query string empty?
         */
        get() = query.isEmpty()

    /**
     * Does this query have any arguments?
     */
    public val hasArguments: Boolean
        get() = !arguments.isEmpty()

    /**
     * Adds an argument to this query.
     */
    @IgnorableReturnValue
    public fun addArgument(argument: Any?): QueryBuilder {
        arguments.add(argument)
        return this
    }

    /**
     * Adds given arguments to this query.
     */
    @IgnorableReturnValue
    public fun addArguments(vararg args: Any?): QueryBuilder {
        return addArguments(args.asList())
    }

    /**
     * Adds given arguments to this query.
     */
    @IgnorableReturnValue
    public fun addArguments(args: Collection<*>): QueryBuilder {
        arguments.addAll(args)
        return this
    }

    /**
     * Builds an SQL query from the current state of this builder.
     * 
     * @throws IllegalStateException if the builder is empty
     */
    public fun build(): SqlQuery {
        check(!query.isEmpty()) { "empty query" }

        return SqlQuery(query.toString(), arguments)
    }

    public companion object {
        /** Placeholder to be used in queries for values  */
        internal const val PLACEHOLDER: String = "?"
    }
}
