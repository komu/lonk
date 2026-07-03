package dev.komu.lonk

import dev.komu.lonk.result.*
import dev.komu.lonk.testutils.DatabaseProvider.POSTGRESQL
import dev.komu.lonk.testutils.DatabaseTest
import dev.komu.lonk.testutils.transactionalTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@DatabaseTest(POSTGRESQL)
internal class DbConnectionTest(private val db: DbConnectionProvider) {

    @Test
    fun `primitive queries`() = transactionalTest(db) { db ->
        assertEquals(42, db.query("values (42)").findUnique<Int>())
        assertEquals(42, db.query("values (42)").findUnique(Int::class))

        assertEquals(42L, db.query("values (cast(42 as bigint))").findUnique<Long>())
        assertEquals(42.0f, db.query("values (42.0)").findUnique<Float>())
        assertEquals(42.0, db.query("values (42.0)").findUnique<Double>())
        assertEquals("foo", db.query("values ('foo')").findUnique<String>())
        assertEquals(true, db.query("values (true)").findUnique<Boolean>())
        assertNull(db.query("values (cast(null as boolean))").findUniqueNullable<Boolean>())
    }

    @Test
    fun `big numbers`() = transactionalTest(db) { db ->
        assertEquals(
            BigDecimal("4242242848428484848484848"),
            db.query("values (4242242848428484848484848)").findUnique<BigDecimal>()
        )
    }

    @Test
    fun `auto-detecting types`() = transactionalTest(db) { db ->
        assertEquals(42, db.query("values (42)").findUnique<Any>())
        assertEquals("foo", db.query("values ('foo')").findUnique<Any>())
        assertEquals(true, db.query("values (true)").findUnique<Any>())
    }

    @Test
    fun `constructor row mapping`() = transactionalTest(db) { db ->
        val departments = db.query("select * from (values (1, 'foo'), (2, 'bar')) d").findAll<Department>()

        assertEquals(2, departments.size)
        assertEquals(1, departments[0].id)
        assertEquals("foo", departments[0].name)
        assertEquals(2, departments[1].id)
        assertEquals("bar", departments[1].name)
    }

    @Test
    fun `findUnique single result`() = transactionalTest(db) { db ->
        assertEquals(42, db.query("values (42)").findUnique<Int>())
    }

    @Test
    fun `findUnique non-unique result`() = transactionalTest(db) { db ->
        assertFailsWith<NonUniqueResultException> {
            db.query("VALUES (1), (2)").findUnique<Int>()
        }
    }

    @Test
    fun `findUnique empty result`() = transactionalTest(db) { db ->
        assertFailsWith<EmptyResultException> {
            db.query("SELECT * FROM (VALUES (1)) n WHERE FALSE").findUnique<Int>()
        }
    }

    @Test
    fun `findUniqueOrNull single result`() = transactionalTest(db) { db ->
        assertEquals(42, db.query("values (42)").findUniqueNullable<Int>())
    }

    @Test
    fun `findNullableUnique non-unique result`() = transactionalTest(db) { db ->
        assertFailsWith<NonUniqueResultException> {
            db.query("values (1), (2)").findUniqueNullable<Int>()
        }
    }

    @Test
    fun `findOptional empty result`() = transactionalTest(db) { db ->
        assertNull(db.query("select * from (values (1)) n where false").findOptional<Int>())
    }

    @Test
    fun `findUniqueNullable null result`() = transactionalTest(db) { db ->
        assertNull(db.query("values (cast (null as int))").findUniqueNullable<Int>())
    }

    @Test
    fun rowMapper() = transactionalTest(db) { db ->
        val squaringRowMapper: ResultRowMapper<Int> = { row ->
            val value = row.get<Int>(0)
            value * value
        }

        assertEquals(listOf(1, 4, 9), db.query("values (1), (2), (3)").findAll(squaringRowMapper))
        assertEquals(49, db.query("values (7)").findUnique(squaringRowMapper))
        assertNull(db.query("select * from (values (1)) n where false").findOptional(squaringRowMapper))
    }

    @Test
    fun `custom result processor`() = transactionalTest(db) { db ->
        val rowCounter = object : ResultRowCollector<Int> {
            private var rows = 0
            override fun accumulate(row: ResultRow): Boolean {
                rows++
                return true
            }

            override fun finish() = rows
        }

        assertEquals(3, db.query("values (1), (2), (3)").collect(rowCounter))
    }

    @BeforeEach
    fun createTable(): Unit = runBlocking {
        db.withTransaction { conn ->
            conn.update("drop table if exists dc_test")
            conn.update("create table dc_test (value varchar(64))")
        }
    }

    @Test
    fun `changes are committed on close`() = runBlocking {
        val _ = db.withTransaction { conn ->
            conn.update("insert into dc_test values ('hello')")
        }

        db.withTransaction { conn ->
            assertEquals("hello", conn.query("select value from dc_test").findUnique<String>())
        }
    }

    @Test
    fun `changes are rolled back on exceptions rollback-only`() = runBlocking {
        assertFailsWith<RuntimeException> {
            db.withTransaction { conn ->
                conn.update("insert into dc_test values ('rolled_back')")
                throw RuntimeException()
            }
        }
        db.withTransaction { conn ->
            assertEquals(0, conn.query("select value from dc_test").findAll<String>().size)
        }
    }

    @Test
    fun `multiple operations share the same connection`() = runBlocking {
        val _ = db.withTransaction { conn ->
            conn.update("insert into dc_test values ('foo')")
            conn.update("insert into dc_test values ('bar')")
        }

        db.withTransaction { conn ->
            assertEquals(2, conn.query("select value from dc_test").findAll<String>().size)
        }
    }

    class Department(val id: Int, val name: String)
}
