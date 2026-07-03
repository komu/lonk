@file:Suppress("SqlResolve")

package dev.komu.lonk.dialect

import dev.komu.lonk.DbConnectionProvider
import dev.komu.lonk.testutils.DatabaseProvider.POSTGRESQL
import dev.komu.lonk.testutils.DatabaseTest
import dev.komu.lonk.testutils.transactionalTest
import org.junit.jupiter.api.Assertions.assertArrayEquals
import kotlin.test.Test
import kotlin.test.assertEquals

@DatabaseTest(POSTGRESQL)
internal class PostgreSQLLargeObjectTest(private val db: DbConnectionProvider) {

    @Test
    fun `stream blob to database byte array`() = transactionalTest(db) { db ->
        db.update("drop table if exists blob_test")
        db.update("create temporary table blob_test (id int, blob_data bytea)")

        val originalData = byteArrayOf(25, 35, 3)
        db.update("insert into blob_test values (1, ?)", originalData.inputStream())

        val data = db.query("select blob_data from blob_test where id=1").findUnique<ByteArray>()
        assertArrayEquals(originalData, data)
    }

    @Test
    fun `stream reader to database text`() = transactionalTest(db) { db ->
        db.update("drop table if exists text_test")
        db.update("create temporary table text_test (id int, text_data text)")

        val originalData = "foo"
        db.update("insert into text_test values (1, ?)", originalData.reader())

        val data = db.query("select text_data from text_test where id=1").findUnique<String>()
        assertEquals(originalData, data)
    }
}
