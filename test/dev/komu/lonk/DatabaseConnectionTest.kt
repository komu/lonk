package dev.komu.lonk

import dev.komu.lonk.testutils.DatabaseProvider.POSTGRESQL
import dev.komu.lonk.testutils.DatabaseTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals

@DatabaseTest(POSTGRESQL)
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
}
