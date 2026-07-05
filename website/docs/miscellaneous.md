## Miscellaneous features

### Explicit instantiators

Normally, Lonk will automatically detect the best way to instantiate your classes based on database results. If a
class has multiple constructors, this can be ambiguous. If you wish to be more explicit, annotate your preferred
constructor --- or a companion object function that returns an instance of the class --- with `@DbInstantiator`.
This causes Lonk to ignore all other constructors. It is an error to annotate more than one constructor or
function this way.

### Custom result collectors

If you need full control over how the whole result set is processed --- rather than just mapping each row on its
own with a `ResultRowMapper` --- you can supply a `ResultRowCollector`. (The example below counts rows just to keep
things simple; in real code you'd just write `select count(*)`.)

```kotlin
val rowCount = conn.query("select * from department").collect(object : ResultRowCollector<Int> {
    private var rows = 0

    override fun accumulate(row: ResultRow): Boolean {
        rows++
        return true
    }

    override fun finish() = rows
})
```
