package dev.komu.lonk

import dev.komu.lonk.instantiation.InstantiatorProvider
import dev.komu.lonk.result.*
import org.intellij.lang.annotations.Language
import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.time.Duration

public abstract class DatabaseConnection internal constructor(
    private val instantiatorRegistry: InstantiatorProvider,
) {

    /**
     * default timeout set on all statements
     */
    public var defaultTimeout: Duration? = null
        set(value) {
            require(value != null && value >= Duration.ZERO) { "Negative timeout: $value" }
            field = value
        }

    public suspend fun <T : Any> executeQuery(
        @Language("SQL") sql: String,
        vararg args: Any?,
        resultSetProcessor: (ResultSet) -> T
    ): T =
        executeQuery(query(sql, *args), resultSetProcessor)

    public suspend fun <T : Any> executeQuery(query: SqlQuery, resultSetProcessor: (ResultSet) -> T): T =
        executeQuery({ resultSetProcessor(it) }, query)

    /**
     * Executes a query and processes the results with given [ResultSetProcessor].
     * All other findXXX-methods are just convenience methods for this one.
     */
    public abstract suspend fun <T> executeQuery(processor: ResultSetProcessor<T>, query: SqlQuery): T

    /**
     * Executes a query and processes the results with given [ResultSetProcessor].
     * 
     * @see .executeQuery
     */
    public suspend fun <T> executeQuery(
        processor: ResultSetProcessor<T>,
        @Language("SQL") sql: String,
        vararg args: Any?
    ): T =
        executeQuery(processor, query(sql, *args))

    /**
     * Executes a query and processes each row of the result with given [RowMapper]
     * to produce a list of results.
     */
    public suspend fun <T> findAll(rowMapper: RowMapper<T>, query: SqlQuery): List<T> =
        executeQuery(rowMapper.list(), query)

    public suspend fun <T : Any> findAll(
        @Language("SQL") sql: String,
        vararg args: Any?,
        rowMapper: (ResultSet) -> T
    ): List<T> =
        findAll(query(sql, *args), rowMapper)

    public suspend fun <T : Any> findAll(query: SqlQuery, rowMapper: (ResultSet) -> T): List<T> =
        findAll({ rowMapper(it) }, query)


    /**
     * Executes a query and processes each row of the result with given [RowMapper]
     * to produce a list of results.
     */
    public suspend fun <T> findAll(rowMapper: RowMapper<T>, @Language("SQL") sql: String, vararg args: Any?): List<T> =
        findAll(rowMapper, query(sql, *args))

    /**
     * Executes a query and converts the results to instances of the given class using default mechanisms.
     */
    public suspend inline fun <reified T : Any> findAll(@Language("SQL") sql: String, vararg args: Any?): List<T> =
        findAll(query(sql, *args))

    /**
     * Executes a query and converts the results to instances of given class using default mechanisms.
     */
    public suspend inline fun <reified T : Any> findAll(query: SqlQuery): List<T> =
        findAll(T::class, query)

    /**
     * Executes a query and converts the results to instances of given class using default mechanisms.
     */
    public suspend fun <T : Any> findAll(cl: KClass<T>, query: SqlQuery): List<T> =
        executeQuery<List<T>>(resultProcessorForClass(cl), query)

    public suspend fun <T : Any> findAll(cl: KClass<T>, @Language("SQL") sql: String, vararg args: Any?): List<T> =
        findAll(cl, query(sql, *args))

    public suspend fun <T : Any> findUnique(
        @Language("SQL") sql: String,
        vararg args: Any?,
        rowMapper: (ResultSet) -> T
    ): T =
        findUnique(query(sql, *args), rowMapper)

    public suspend fun <T : Any> findUnique(query: SqlQuery, rowMapper: (ResultSet) -> T): T =
        findUnique({ rowMapper(it) }, query)

    public suspend fun <T> findUnique(mapper: RowMapper<T>, query: SqlQuery): T = executeQuery(mapper.unique(), query)

    public suspend fun <T> findUnique(mapper: RowMapper<T>, @Language("SQL") sql: String, vararg args: Any?): T =
        findUnique(mapper, query(sql, *args))

    public suspend inline fun <reified T : Any> findUnique(@Language("SQL") sql: String, vararg args: Any?): T =
        findUnique(query(sql, *args))

    public suspend inline fun <reified T : Any> findUnique(query: SqlQuery): T =
        findUnique(T::class, query)

    public suspend fun <T : Any> findUnique(cl: KClass<T>, query: SqlQuery): T =
        executeQuery(rowMapperForClass(cl).unique(), query)

    public suspend fun <T : Any> findUnique(cl: KClass<T>, @Language("SQL") sql: String, vararg args: Any?): T =
        findUnique(cl, query(sql, *args))

    public suspend fun <T : Any> findNullableUnique(
        @Language("SQL") sql: String,
        vararg args: Any?,
        rowMapper: (ResultSet) -> T
    ): T? =
        findNullableUnique(query(sql, *args), rowMapper)

    public suspend fun <T : Any> findNullableUnique(query: SqlQuery, rowMapper: (ResultSet) -> T): T? =
        findNullableUnique({ rowMapper(it) }, query)

    public suspend fun <T : Any> findNullableUnique(rowMapper: RowMapper<T>, query: SqlQuery): T? =
        executeQuery(rowMapper.optional(), query)

    /**
     * Alias for {findUniqueOrNull(rowMapper, SqlQuery.query(sql, args))}.
     */
    public suspend fun <T : Any> findNullableUnique(
        rowMapper: RowMapper<T>,
        @Language("SQL") sql: String,
        vararg args: Any?
    ): T? =
        findNullableUnique(rowMapper, query(sql, *args))

    public suspend inline fun <reified T : Any> findUniqueOrNull(@Language("SQL") sql: String, vararg args: Any?): T? =
        findUniqueOrNull(query(sql, *args))

    public suspend inline fun <reified T : Any> findUniqueOrNull(query: SqlQuery): T? =
        findUniqueOrNull(T::class, query)

    public suspend fun <T : Any> findUniqueOrNull(cl: KClass<T>, @Language("SQL") sql: String, vararg args: Any?): T? =
        findUniqueOrNull(cl, query(sql, *args))

    public suspend fun <T : Any> findUniqueOrNull(cl: KClass<T>, query: SqlQuery): T? =
        executeQuery(rowMapperForClass(cl).optional(), query)

    public suspend inline fun <reified T : Any> findNullableUnique(
        @Language("SQL") sql: String,
        vararg args: Any?
    ): T? =
        findNullableUnique(query(sql, *args))

    public suspend inline fun <reified T : Any> findNullableUnique(query: SqlQuery): T? =
        findNullableUnique(T::class, query)

    public suspend fun <T : Any> findNullableUnique(cl: KClass<T>, query: SqlQuery): T? =
        executeQuery(nullableRowMapperForClass(cl).unique(), query)

    public suspend fun <T : Any> findNullableUnique(
        cl: KClass<T>,
        @Language("SQL") sql: String,
        vararg args: Any?
    ): T? =
        findNullableUnique(cl, query(sql, *args))

    /**
     * A convenience method for retrieving a single non-null integer.
     * 
     * @throws NonUniqueResultException if there is more then one row
     * @throws EmptyResultException     if there are no rows
     */
    public suspend fun findUniqueInt(query: SqlQuery): Int =
        executeQuery(rowMapperForClass(Int::class).unique(), query)

    /**
     * A convenience method for retrieving a single non-null long.
     * 
     * @throws NonUniqueResultException if there is more then one row
     * @throws EmptyResultException     if there are no rows
     */
    public suspend fun findUniqueLong(query: SqlQuery): Long =
        executeQuery(rowMapperForClass(Long::class).unique(), query)

    /**
     * A convenience method for retrieving a single non-null long.
     * 
     * @throws NonUniqueResultException if there is more then one row
     * @throws EmptyResultException     if there are no rows
     */
    public suspend fun findUniqueLong(@Language("SQL") sql: String, vararg args: Any?): Long =
        findUniqueLong(query(sql, *args))

    /**
     * Executes an update against the database and returns the number of affected rows.
     */
    @IgnorableReturnValue
    public abstract suspend fun update(query: SqlQuery): Int

    /**
     * Executes an update against the database and returns the amount of affected rows.
     */
    @IgnorableReturnValue
    public suspend fun update(@Language("SQL") sql: String, vararg args: Any?): Int =
        update(query(sql, *args))

    private fun <T : Any> resultProcessorForClass(cl: KClass<T>): ResultSetProcessor<List<T>> {
        return rowMapperForClass(cl).list()
    }

    private fun <T : Any> rowMapperForClass(cl: KClass<T>): RowMapper<T> {
        return InstantiatorRowMapper(cl, instantiatorRegistry)
    }

    private fun <T : Any> nullableRowMapperForClass(cl: KClass<T>): RowMapper<T?> {
        return NullableInstantiatorRowMapper(cl, instantiatorRegistry)
    }

    public var isRollbackOnly: Boolean = false
        private set

    /**
     * Marks the current transaction as rollback-only, indicating that the transaction
     * should be rolled back rather than committed when it is finalized.
     */
    public fun setRollbackOnly() {
        this.isRollbackOnly = true
    }

    /**
     * Commits the pending transaction and closes the underlying JDBC connection.
     *
     * @throws DatabaseException if the commit, rollback, or close raises a SQL error
     */
    public abstract suspend fun close()
}
