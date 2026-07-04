package dev.komu.lonk

import dev.komu.lonk.adapter.DatabaseQuery
import dev.komu.lonk.instantiation.InstantiatorProvider
import dev.komu.lonk.result.*
import org.intellij.lang.annotations.Language
import kotlin.reflect.KClass

/**
 * An open connection to the database, used to run queries and updates.
 *
 * @see DbConnectionProvider
 */
public abstract class DbConnection internal constructor(
    private val instantiatorProvider: InstantiatorProvider,
) {

    /** Binds [sql] and [args] into a [BoundQuery] ready to be executed. */
    public fun query(@Language("SQL") sql: String, vararg args: Any?): BoundQuery =
        query(SqlQuery(sql, args.asList()))

    /** Binds [query] into a [BoundQuery] ready to be executed. */
    public fun query(query: SqlQuery): BoundQuery =
        BoundQuery(this, query)

    /** Commit the current transaction. */
    public abstract suspend fun commit()

    /** Roll back the current transaction. */
    public abstract suspend fun rollback()

    /** Close this connection */
    public abstract suspend fun close()

    /** Executes an update against the database and returns the number of affected rows. */
    @IgnorableReturnValue
    public suspend fun update(query: SqlQuery): Long =
        update(toDatabase(query))

    /** Executes an update against the database and returns the number of affected rows. */
    @IgnorableReturnValue
    public suspend fun update(@Language("SQL") sql: String, vararg args: Any?): Long =
        update(SqlQuery(sql, args.asList()))

    internal abstract suspend fun <T> executeQuery(query: DatabaseQuery, collector: ResultRowCollector<T>): T
    internal abstract suspend fun update(query: DatabaseQuery): Long

    private suspend fun <T> executeQuery(query: SqlQuery, collector: ResultRowCollector<T>): T =
        executeQuery(toDatabase(query), collector)

    private fun toDatabase(query: SqlQuery) = DatabaseQuery(
        sql = query.sql,
        arguments = query.arguments.map { instantiatorProvider.valueToDatabase(it) },
    )

    /** A query bound to a connection and its arguments, ready to be executed. */
    public class BoundQuery internal constructor(private val c: DbConnection, private val query: SqlQuery) {
        /** Executes the query and maps every row with [rowMapper]. */
        public suspend fun <T> findAll(rowMapper: ResultRowMapper<T>): List<T> =
            collect(ListRowCollector(rowMapper))

        /** Executes the query and instantiates every row as [cl]. */
        public suspend fun <T : Any> findAll(cl: KClass<T>): List<T> =
            findAll(InstantiatorRowMapper(cl, c.instantiatorProvider))

        /** Executes the query and instantiates every row as [T]. */
        public suspend inline fun <reified T : Any> findAll(): List<T> =
            findAll(T::class)

        /** Executes the query and maps its single row with [rowMapper], failing unless exactly one row was returned. */
        public suspend fun <T : Any> findUnique(rowMapper: ResultRowMapper<T>): T =
            collect(UniqueRowCollector(rowMapper))

        /** Executes the query and instantiates its single row as [cl], failing unless exactly one row was returned. */
        public suspend fun <T : Any> findUnique(cl: KClass<T>): T =
            findUnique(InstantiatorRowMapper(cl, c.instantiatorProvider))

        /** Executes the query and instantiates its single row as [T], failing unless exactly one row was returned. */
        public suspend inline fun <reified T : Any> findUnique(): T =
            findUnique(T::class)

        /** Executes the query and maps its row with [rowMapper], returning `null` if there were no rows. Fails if there is more than one row. */
        public suspend fun <T : Any> findOptional(rowMapper: ResultRowMapper<T>): T? =
            collect(OptionalRowCollector(rowMapper))

        /** Executes the query and instantiates its row as [cl], returning `null` if there were no rows. Fails if there is more than one row. */
        public suspend fun <T : Any> findOptional(cl: KClass<T>): T? =
            findOptional(InstantiatorRowMapper(cl, c.instantiatorProvider))

        /** Executes the query and instantiates its row as [T], returning `null` if there were no rows. Fails if there is more than one row. */
        public suspend inline fun <reified T : Any> findOptional(): T? =
            findOptional(T::class)

        /** Executes the given query and processes the results with the given [collector] */
        public suspend fun <T> collect(collector: ResultRowCollector<T>): T =
            c.executeQuery(query, collector)
    }
}

