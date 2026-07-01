package dev.komu.lonk

import dev.komu.lonk.testutils.DatabaseProvider.HSQL
import dev.komu.lonk.testutils.DatabaseTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

@DatabaseTest(HSQL)
internal class DatabaseConnectionTest(private val source: DatabaseSource) {

    @BeforeEach
    fun createTable(): Unit = runBlocking {
        source.withConnection { conn ->
            conn.update("drop table if exists dc_test")
            conn.update("create table dc_test (value varchar(64))")
        }
    }

    @Test
    fun `changes are committed on close`() = runBlocking {
        val _ = source.withConnection { conn ->
            conn.update("insert into dc_test values ('hello')")
        }

        source.withConnection { conn ->
            assertEquals("hello", conn.findUnique(String::class, "select value from dc_test"))
        }
    }

    @Test
    fun `changes are rolled back on close when marked rollback-only`() = runBlocking {
        source.withConnection { conn ->
            conn.update("insert into dc_test values ('rolled_back')")
            conn.setRollbackOnly()
        }
        source.withConnection { conn ->
            assertEquals(0, conn.findAll(String::class, "select value from dc_test").size)
        }
    }

    @Test
    fun `multiple operations share the same connection`() = runBlocking {
        val _ = source.withConnection { conn ->
            conn.update("insert into dc_test values ('foo')")
            conn.update("insert into dc_test values ('bar')")
        }

        source.withConnection { conn ->
            assertEquals(2, conn.findAll(String::class, "select value from dc_test").size)
        }
    }

    @Test
    fun `exception during query does not prevent close from committing`() = runBlocking {
        val _ = source.withConnection { conn ->
            conn.update("insert into dc_test values ('committed')")
            assertFails { conn.findUnique(String::class, "select value from nonexistent_table") }
        }

        // The insert before the failing query was committed (no automatic rollback on exception)
        source.withConnection { conn ->
            assertEquals(1, conn.findAll(String::class, "select value from dc_test").size)
        }
    }
}
