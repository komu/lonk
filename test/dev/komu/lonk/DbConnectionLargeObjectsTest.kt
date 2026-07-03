package dev.komu.lonk

import dev.komu.lonk.testutils.DatabaseProvider.POSTGRESQL
import dev.komu.lonk.testutils.DatabaseTest
import dev.komu.lonk.testutils.transactionalTest
import org.junit.jupiter.api.Assertions.assertArrayEquals
import kotlin.test.Test
import kotlin.test.assertEquals

@DatabaseTest(POSTGRESQL)
internal class DbConnectionLargeObjectsTest(private val db: DbConnectionProvider) {

    @Test
    fun `clob columns can be coerced to strings`() = transactionalTest(db) { db ->
        assertEquals("foo", db.query("values (cast ('foo' as text))").findUnique<String>())
    }

    @Test
    fun `blob columns can be coerced to strings`() = transactionalTest(db) { db ->
        val data = byteArrayOf(1, 2, 3)
        assertArrayEquals(data, db.query("values (cast (? as bytea))", data).findUnique<ByteArray>())
    }

    @Test
    fun `stream clob to database`() = transactionalTest(db) { db ->
        db.update("drop table if exists clob_test")
        db.update("create temporary table clob_test (id int, clob_data text)")

        val originalData = "foobar"
        db.update("insert into clob_test values (1, ?)", originalData.reader())

        assertEquals(originalData, db.query("select clob_data from clob_test where id=1").findUnique<String>())
    }

    @Test
    fun `stream blob to database`() = transactionalTest(db) { db ->
        db.update("drop table if exists blob_test")
        db.update("create temporary table blob_test (id int, blob_data bytea)")

        val originalData = byteArrayOf(25, 35, 3)
        db.update("insert into blob_test values (1, ?)", originalData.inputStream())

        assertArrayEquals(originalData, db.query("select blob_data from blob_test where id=1").findUnique<ByteArray>())
    }
}
