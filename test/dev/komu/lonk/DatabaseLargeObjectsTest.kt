package dev.komu.lonk

import dev.komu.lonk.testutils.DatabaseProvider.HSQL
import dev.komu.lonk.testutils.DatabaseTest
import dev.komu.lonk.testutils.transactionalTest
import org.junit.jupiter.api.Assertions.assertArrayEquals
import java.io.InputStream
import java.io.Reader
import kotlin.test.Test
import kotlin.test.assertEquals

@DatabaseTest(HSQL)
internal class DatabaseLargeObjectsTest(private val db: DatabaseSource) {

    @Test
    fun `clob columns can be coerced to strings`() = transactionalTest(db) { db ->
        assertEquals("foo", db.findUnique(String::class, "values (cast ('foo' as clob))"))
    }

    @Test
    fun `blob columns can be coerced to strings`() = transactionalTest(db) { db ->
        val data = byteArrayOf(1, 2, 3)
        assertArrayEquals(data, db.findUnique(ByteArray::class, "values (cast (? as blob))", data))
    }

    @Test
    fun `stream clob to database`() = transactionalTest(db) { db ->
        db.update("drop table if exists clob_test")
        db.update("create temporary table clob_test (id int, clob_data clob)")

        val originalData = "foobar"
        db.update("insert into clob_test values (1, ?)", originalData.reader())

        assertEquals(originalData, db.findUnique(String::class, "select clob_data from clob_test where id=1"))
    }

    @Test
    fun `stream clob from database`() = transactionalTest(db) { db ->
        db.update("drop table if exists clob_test")
        db.update("create temporary table clob_test (id int, clob_data clob)")

        val originalData = "foobar"
        db.update("insert into clob_test values (1, ?)", originalData.reader())

        db.findUnique(Reader::class, "select clob_data from clob_test where id=1").use { reader ->
            assertEquals(originalData, reader.readText())
        }
    }

    @Test
    fun `stream blob to database`() = transactionalTest(db) { db ->
        db.update("drop table if exists blob_test")
        db.update("create temporary table blob_test (id int, blob_data blob)")

        val originalData = byteArrayOf(25, 35, 3)
        db.update("insert into blob_test values (1, ?)", originalData.inputStream())

        assertArrayEquals(originalData, db.findUnique(ByteArray::class, "select blob_data from blob_test where id=1"))
    }

    @Test
    fun `stream blob from database`() = transactionalTest(db) { db ->
        db.update("drop table if exists blob_test")
        db.update("create temporary table blob_test (id int, blob_data blob)")

        val originalData = byteArrayOf(25, 35, 3)
        db.update("insert into blob_test values (1, ?)", originalData.inputStream())

        db.findUnique(InputStream::class, "select blob_data from blob_test where id=1").use { stream ->
            assertArrayEquals(originalData, stream.readBytes())
        }
    }
}
