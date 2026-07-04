# Lonk – A simple database library for non-blocking Kotlin

SQL is a great tool for working with relational databases, but JDBC and R2DBC are painful to use directly. Lonk
provides a thin layer over both: you write plain SQL, and Lonk maps the results onto your own classes using
Kotlin reflection, with no annotations or generated code required. Every operation is a suspend function, so it
composes naturally with coroutines.

```
dev.komu.lonk:lonk:<VERSION>
```

## Quick example

```kotlin
val db = JdbcConnectionProvider(dataSource)

val departments = db.withTransaction { conn ->
    conn.query("select id, name from department order by name").findAll<Department>()
}
```

Where `Department` is just a plain class with a matching constructor:

```kotlin
class Department(val id: Int, val name: String)
```

See the [documentation](https://komu.github.io/lonk/) to get started.
