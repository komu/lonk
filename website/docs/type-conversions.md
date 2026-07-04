## Type conversions

### Custom type conversions

Sometimes you need to convert database values to your own custom types and vice versa. To do that, register
conversions when configuring your `DbConnectionProvider`:

```kotlin
val db = JdbcConnectionProvider(dataSource) {
    conversions {
        // register conversions from database and to database types separately
        registerConversionFromDatabase(EmailAddress::parse)
        registerConversionToDatabase(EmailAddress::toString)

        // or register both directions with one call
        registerConversions<String, EmailAddress>(
            fromDatabase = EmailAddress::parse,
            toDatabase = EmailAddress::toString,
        )
    }
}
```

### Enums

By default, enums are persisted using a value you choose --- for example their name or ordinal:

```kotlin
val db = JdbcConnectionProvider(dataSource) {
    conversions {
        registerEnum(MyEnum::name)
    }
}
```

The value doesn't need to be the name or ordinal --- any property works, so you can persist a stable code that's
independent of how the enum constants happen to be named or ordered in your Kotlin code:

```kotlin
enum class Status(val code: String) {
    ACTIVE("A"),
    INACTIVE("I"),
    PENDING("P"),
}

val db = JdbcConnectionProvider(dataSource) {
    conversions {
        registerEnum(Status::code)
    }
}
```
