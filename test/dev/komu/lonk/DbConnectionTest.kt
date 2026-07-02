package dev.komu.lonk

import dev.komu.lonk.testutils.DatabaseProvider.POSTGRESQL
import dev.komu.lonk.testutils.DatabaseTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@DatabaseTest(POSTGRESQL)
internal class DbConnectionTest(private val source: DbConnectionProvider) {

    @BeforeEach
    fun createTable(): Unit = runBlocking {
        source.withTransaction { conn ->
            conn.update("drop table if exists dc_test")
            conn.update("create table dc_test (value varchar(64))")
        }
    }

    @Test
    fun `changes are committed on close`() = runBlocking {
        val _ = source.withTransaction { conn ->
            conn.update("insert into dc_test values ('hello')")
        }

        source.withTransaction { conn ->
            assertEquals("hello", conn.findUnique(String::class, "select value from dc_test"))
        }
    }

    @Test
    fun `changes are rolled back on exceptions rollback-only`() = runBlocking {
        assertFailsWith<RuntimeException> {
            source.withTransaction { conn ->
                conn.update("insert into dc_test values ('rolled_back')")
                throw RuntimeException()
            }
        }
        source.withTransaction { conn ->
            assertEquals(0, conn.findAll(String::class, "select value from dc_test").size)
        }
    }

    @Test
    fun `multiple operations share the same connection`() = runBlocking {
        val _ = source.withTransaction { conn ->
            conn.update("insert into dc_test values ('foo')")
            conn.update("insert into dc_test values ('bar')")
        }

        source.withTransaction { conn ->
            assertEquals(2, conn.findAll(String::class, "select value from dc_test").size)
        }
    }
}
