package dev.komu.lonk

import dev.komu.lonk.result.ResultRow
import dev.komu.lonk.result.ResultRowCollector
import dev.komu.lonk.result.ResultRowMapper
import dev.komu.lonk.result.get
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
    fun `integer queries`() = transactionalTest(db) { db ->
        assertEquals(42, db.query("values (42)").findUnique<Int>())
        assertEquals(42, db.query("values (42)").findUnique(Int::class))
    }

    @Test
    fun `boolean queries`() = transactionalTest(db) { db ->
        assertEquals(true, db.query("values (true)").findUnique<Boolean>())
    }

    @Test
    fun `string queries`() = transactionalTest(db) { db ->
        assertEquals("foo", db.query("values ('foo')").findUnique<String>())
    }

    @Test
    fun `long queries`() = transactionalTest(db) { db ->
        assertEquals(42L, db.query("values (cast(42 as bigint))").findUnique<Long>())
    }

    @Test
    fun `double queries`() = transactionalTest(db) { db ->
        assertEquals(42.0, db.query("values (cast(42.0 as double precision))").findUnique<Double>())
    }

    @Test
    fun `decimal queries`() = transactionalTest(db) { db ->
        val expected = BigDecimal(42.0)
        val actual = db.query("values (cast(42.0 as decimal))").findUnique<BigDecimal>()
        assertEquals(0, expected.compareTo(actual))
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
        assertFailsWith<UnexpectedResultException> {
            db.query("VALUES (1), (2)").findUnique<Int>()
        }
    }

    @Test
    fun `findUnique empty result`() = transactionalTest(db) { db ->
        assertFailsWith<UnexpectedResultException> {
            db.query("SELECT * FROM (VALUES (1)) n WHERE FALSE").findUnique<Int>()
        }
    }

    @Test
    fun `findOptional empty result`() = transactionalTest(db) { db ->
        assertNull(db.query("select * from (values (1)) n where false").findOptional<Int>())
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
    fun createTable(): Unit = transactionalTest(db) { conn ->
        conn.update("drop table if exists dc_test")
        conn.update("create table dc_test (value varchar(64))")
    }

    @Test
    fun `null argument binds and round-trips through a table column`() = transactionalTest(db) { conn ->
        conn.update("insert into dc_test values (?)", null)

        assertEquals(NullableValue(null), conn.query("select value from dc_test").findUnique<NullableValue>())
    }

    data class NullableValue(val value: String?)

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
    @Suppress("SqlWithoutWhere")
    fun `update returns the number of affected rows`() = transactionalTest(db) { conn ->
        assertEquals(1L, conn.update("insert into dc_test values ('a')"))
        assertEquals(2L, conn.update("insert into dc_test values ('b'), ('c')"))
        assertEquals(3L, conn.update("update dc_test set value = 'x'"))
        assertEquals(3L, conn.update("delete from dc_test"))
        assertEquals(0L, conn.update("delete from dc_test"))
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

    @Test
    fun findAll() = transactionalTest(db) { db ->
        assertEquals(
            listOf(Department(1, "foo"), Department(2, "bar")),
            db.query("select * from (values (1, 'foo'), (2, 'bar')) d").findAll<Department>()
        )
    }

    @Test
    fun findUnique() = transactionalTest(db) { db ->
        assertEquals(Department(1, "foo"), db.query("select * from (values (1, 'foo')) d").findUnique<Department>())
    }

    @Test
    fun `findUnique custom mapper`() = transactionalTest(db) { db ->
        val result = db.query("select * from (values (1, 'foo')) d").findUnique { row ->
            row.get<Int>(0) to row.get<String>(1).reversed()
        }

        assertEquals(1 to "oof", result)
    }

    @Test
    fun `findOptional existing result`() = transactionalTest(db) { db ->
        assertEquals(Department(1, "foo"), db.query("select * from (values (1, 'foo')) d").findOptional<Department>())
    }

    @Test
    fun `findOptional nonexistent result`() = transactionalTest(db) { db ->
        @Suppress("SqlConstantExpression")
        assertNull(db.query("select * from (values (1, 'foo')) d where 1 = 2").findOptional<Department>())
    }

    @Test
    fun executeQuery() = transactionalTest(db) { db ->
        val processor = object : ResultRowCollector<Pair<List<Int>, List<String>>> {
            private val ints = mutableListOf<Int>()
            private val strs = mutableListOf<String>()

            override fun accumulate(row: ResultRow): Boolean {
                ints += row.get<Int>(0)
                strs += row.get<String>(1)
                return true
            }

            override fun finish() = ints to strs
        }

        val result = db.query("select * from (values (1, 'foo'), (2, 'bar')) d").collect(processor)

        assertEquals(listOf(1, 2) to listOf("foo", "bar"), result)
    }

    data class Department(val id: Int, val name: String)
}
