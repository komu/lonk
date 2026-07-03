package dev.komu.lonk

import dev.komu.lonk.testutils.DatabaseProvider.POSTGRESQL
import dev.komu.lonk.testutils.DatabaseTest
import dev.komu.lonk.testutils.transactionalTest
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertNull

@DatabaseTest(POSTGRESQL)
internal class DbConnectoinReflectiveInstantiationTest(private val db: DbConnectionProvider) {

    @Test
    fun `constructor binding with null values and conversions`() = transactionalTest(db) { db ->
        val result = db.query("values (cast(null as timestamp))").findUnique<ConstructorNeedingConversion>()
        assertNull(result.dateTime)
    }

    class ConstructorNeedingConversion(val dateTime: LocalDateTime?)
}
