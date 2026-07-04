## Transactions

### Transaction blocks

To perform a bunch of operations in a transaction, use `DbConnectionProvider.withTransaction`:

```kotlin
db.withTransaction { conn ->
    conn.update("insert into department (name) values (?)", "Sales")
    conn.update("insert into department (name) values (?)", "Marketing")
}
```

The transaction is committed once the block completes normally, and rolled back if it throws. The connection is
closed either way. Every call made against the `conn` passed to the block participates in the same transaction, so
you can freely mix queries and updates.

### Manual control over commit and rollback

If you want manual control over when the transaction is committed or rolled back, use `withConnection(autoCommit =
false)` instead of `withTransaction`. It still opens and closes the connection for you, but leaves committing and
rolling back to you:

```kotlin
db.withConnection(autoCommit = false) { conn ->
    conn.update("insert into department (name) values (?)", "Sales")
    conn.commit()
}
```

If you don't want transactional semantics at all --- each statement committed as it executes, with nothing for you
to commit or roll back --- pass `autoCommit = true` instead:

```kotlin
db.withConnection(autoCommit = true) { conn ->
    conn.query("select count(*) from department").findUnique<Int>()
}
```

### Manual control over the connection

If you need to manage the connection's lifecycle yourself too, call `openConnection` directly and take care of
committing, rolling back and closing:

```kotlin
val conn = db.openConnection(autoCommit = false)
try {
    conn.update("insert into department (name) values (?)", "Sales")
    conn.commit()
} catch (e: Throwable) {
    conn.rollback()
    throw e
} finally {
    conn.close()
}
```

This is exactly what `withTransaction` does for you, so prefer it unless you have a good reason not to.
