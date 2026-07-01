package dev.komu.lonk

import dev.komu.lonk.testutils.DatabaseProvider.POSTGRESQL
import dev.komu.lonk.testutils.DatabaseTest
import dev.komu.lonk.testutils.transactionalTest
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertNull

@DatabaseTest(POSTGRESQL)
internal class DatabaseReflectiveInstantiationTest(private val db: DatabaseSource) {

    @Test
    fun `constructor binding with null values and conversions`() = transactionalTest(db) { db ->
        val result = db.findUnique(ConstructorNeedingConversion::class, "values (cast(null as timestamp))")
        assertNull(result.dateTime)
    }

    class ConstructorNeedingConversion(val dateTime: Instant?)
}
