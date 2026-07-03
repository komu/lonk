package dev.komu.lonk

import dev.komu.lonk.instantiation.InstantiatorProvider
import dev.komu.lonk.result.*
import org.intellij.lang.annotations.Language
import kotlin.reflect.KClass

public abstract class DbConnection internal constructor(
    private val instantiatorProvider: InstantiatorProvider,
) {

    internal abstract suspend fun <T> doExecuteQuery(processor: ResultAggregator<T>, query: DatabaseQuery): T
    internal abstract suspend fun doUpdate(query: DatabaseQuery): Long

    public abstract suspend fun commit()
    public abstract suspend fun rollback()

    /** Close this connection */
    public abstract suspend fun close()

    public suspend fun <T> executeQuery(processor: ResultAggregator<T>, query: SqlQuery): T =
        doExecuteQuery(processor, query.toDatabase())

    public suspend fun <T : Any> executeQuery(query: SqlQuery, resultAggregator: ResultAggregator<T>): T =
        executeQuery(resultAggregator, query)
    /**
     * Executes a query and processes the results with given [ResultAggregator].
     * 
     * @see .executeQuery
     */
    public suspend fun <T> executeQuery(
        processor: ResultAggregator<T>,
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
        rowMapper: (ResultRow) -> T
    ): List<T> =
        findAll(query(sql, *args), rowMapper)

    public suspend fun <T : Any> findAll(query: SqlQuery, rowMapper: (ResultRow) -> T): List<T> =
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
     * Executes a query and converts the results to instances of the given class using default mechanisms.
     */
    public suspend inline fun <reified T : Any> findAll(query: SqlQuery): List<T> =
        findAll(T::class, query)

    /**
     * Executes a query and converts the results to instances of the given class using default mechanisms.
     */
    public suspend fun <T : Any> findAll(cl: KClass<T>, query: SqlQuery): List<T> =
        executeQuery<List<T>>(resultProcessorForClass(cl), query)

    public suspend fun <T : Any> findAll(cl: KClass<T>, @Language("SQL") sql: String, vararg args: Any?): List<T> =
        findAll(cl, query(sql, *args))

    public suspend fun <T : Any> findUnique(
        @Language("SQL") sql: String,
        vararg args: Any?,
        rowMapper: (ResultRow) -> T
    ): T =
        findUnique(query(sql, *args), rowMapper)

    public suspend fun <T : Any> findUnique(query: SqlQuery, rowMapper: (ResultRow) -> T): T =
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
        rowMapper: (ResultRow) -> T
    ): T? =
        findNullableUnique(query(sql, *args), rowMapper)

    public suspend fun <T : Any> findNullableUnique(query: SqlQuery, rowMapper: (ResultRow) -> T): T? =
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
    public suspend fun update(query: SqlQuery): Long =
        doUpdate(query.toDatabase())

    /**
     * Executes an update against the database and returns the amount of affected rows.
     */
    @IgnorableReturnValue
    public suspend fun update(@Language("SQL") sql: String, vararg args: Any?): Long =
        update(query(sql, *args))

    private fun <T : Any> resultProcessorForClass(cl: KClass<T>): ResultAggregator<List<T>> {
        return rowMapperForClass(cl).list()
    }

    private fun <T : Any> rowMapperForClass(cl: KClass<T>): RowMapper<T> {
        return InstantiatorRowMapper(cl, instantiatorProvider)
    }

    private fun <T : Any> nullableRowMapperForClass(cl: KClass<T>): RowMapper<T?> {
        return NullableInstantiatorRowMapper(cl, instantiatorProvider)
    }

    private fun SqlQuery.toDatabase() = DatabaseQuery(
        sql = sql,
        arguments = arguments.map { instantiatorProvider.valueToDatabase(it) },
    )
}
