## Basic usage

### Configuring the database connection

Most things in Lonk happen through a `DbConnection`. To get hold of one, you first need a `DbConnectionProvider`:
a factory that shares a single configuration (type conversions and dispatcher/placeholder settings) across every
`DbConnection` it opens. In a typical application you should only configure a single instance --- unless you need
to connect to multiple databases.

If you're using JDBC, create a `JdbcConnectionProvider` out of a `javax.sql.DataSource`. It dispatches blocking
JDBC calls on a `CoroutineDispatcher` (by default `Dispatchers.IO`), and has been tested against PostgreSQL and
HSQLDB, but should work with any JDBC-compliant driver:

```kotlin
val db = JdbcConnectionProvider(dataSource)
```

You can customize the dispatcher, and register [custom type conversions](type-conversions.md#custom-type-conversions),
through the configuration lambda:

```kotlin
val db = JdbcConnectionProvider(dataSource) {
    dispatcher = myDispatcher
    conversions {
        registerConversions(EmailAddress::parse, EmailAddress::toString)
    }
}
```

If you'd rather use R2DBC for fully non-blocking access, create an `R2dbcConnectionProvider` out of a
`io.r2dbc.spi.ConnectionFactory` instead. It has been tested against the PostgreSQL R2DBC driver:

```kotlin
val db = R2dbcConnectionProvider(connectionFactory)
```

With JDBC you always write queries using JDBC-style `?` placeholders, since JDBC itself requires it. R2DBC has no
such standard, so by default you write your queries using whatever parameter syntax your R2DBC driver expects
natively (e.g. `$1`, `$2`, ... for PostgreSQL). If you'd rather write JDBC-style `?` placeholders with R2DBC too,
ask Lonk to translate them into the driver's native style:

```kotlin
val db = R2dbcConnectionProvider(connectionFactory) {
    placeholderTranslation = PlaceholderTranslation.PostgreSQL
}
```

### Opening connections

Once you have a `DbConnectionProvider`, use `withConnection` or `withTransaction` to obtain a `DbConnection` that is
automatically closed once the block completes or throws:

```kotlin
db.withConnection(autoCommit = true) { conn ->
    // operations against conn
}

db.withTransaction { conn ->
    // operations against conn, committed on success, rolled back on exception
}
```

`withTransaction` commits the transaction when the block returns normally and rolls it back if an exception is
thrown, so you rarely need to call `commit()` or `rollback()` yourself. `withConnection` requires you to say
explicitly whether you want autocommit semantics or manual control over commits; if you need more control over the
connection's lifecycle too, you can call `openConnection` directly, but then you're responsible for closing it. See
[Transactions](transactions.md) for the full picture.

### SqlQuery vs. query parameters

`DbConnection.query` comes in two variants: there's an implementation that takes an `SqlQuery` as a parameter and
another that takes a `String` and a variable number of arguments. The latter is just a convenience method for the
former, meaning that the following code fragments are identical in functionality:

```kotlin
val query = SqlQuery("select id, name from department where update_timestamp > ?", listOf(date))
conn.query(query).findAll<Department>()

conn.query("select id, name from department where update_timestamp > ?", date).findAll<Department>()
```

Normally you want to use the latter form, but every once in a while it's useful to be able to pass the query
around with its arguments already bound --- for example when you build the query dynamically and hand it off to
another function:

```kotlin
conn.query(buildDepartmentQuery(form)).findAll<Department>()
```

### Finding stuff

Running queries that return basic types is straightforward:

```kotlin
val newIds: List<Int> = conn.query(
    "select id from department where created_date > ?", date
).findAll()
```

There are a couple of ways to fetch results with multiple columns. First, you could just create a matching
constructor:

```kotlin
val departments = conn.query("select id, name from department").findAll<Department>()

class Department(val id: Int, val name: String)
```

!!! note
    If a class has multiple constructors, Lonk needs to decide which one to use --- see
    [Explicit instantiators](miscellaneous.md#explicit-instantiators) if you need to control that.

### Fetching a single value

Instead of `findAll`, use `findUnique` or `findOptional` depending on how many rows you expect:

```kotlin
// exactly one row, fails otherwise
val department = conn.query("select id, name from department where id=?", id).findUnique<Department>()

// zero or one rows; null if there were none
val maybeDepartment = conn.query("select id, name from department where id=?", id).findOptional<Department>()
```

### Custom row mapping

If you don't want Lonk to instantiate your classes automatically, you can supply your own `ResultRowMapper`:

```kotlin
val squares = conn.query("select value from numbers").findAll { row ->
    val value = row.get<Int>(0)
    value * value
}
```

If you need full control over how the whole result set is processed instead of just mapping each row on its own,
you can supply your own `ResultRowCollector` --- see [Miscellaneous](miscellaneous.md#custom-result-collectors) for
details. Usually this should be unnecessary.

### Updates

Normal updates are straightforward, since we don't need to do much work to map the results:

```kotlin
val modifiedRows: Long = conn.update("delete from user where id=?", 42)
```
