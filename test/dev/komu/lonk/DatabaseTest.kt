package dev.komu.lonk

import dev.komu.lonk.result.*
import dev.komu.lonk.testutils.DatabaseProvider.POSTGRESQL
import dev.komu.lonk.testutils.DatabaseTest
import dev.komu.lonk.testutils.transactionalTest
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@DatabaseTest(POSTGRESQL)
internal class DatabaseTest(private val db: DatabaseSource) {

    @Test
    fun `primitive queries`() = transactionalTest(db) { db ->
        assertEquals(42, db.findUnique<Int>("values (42)"))
        assertEquals(42, db.findUnique(Int::class, "values (42)"))
        assertEquals(42L, db.findUnique(Long::class, "values (cast(42 as bigint))"))
        assertEquals(42.0f, db.findUnique(Float::class, "values (42.0)"))
        assertEquals(42.0, db.findUnique(Double::class, "values (42.0)"))
        assertEquals("foo", db.findUnique(String::class, "values ('foo')"))
        assertEquals(true, db.findUnique(Boolean::class, "values (true)"))
        assertNull(db.findNullableUnique(Boolean::class, "values (cast(null as boolean))"))
    }

    @Test
    fun `big numbers`() = transactionalTest(db) { db ->
        assertEquals(
            BigDecimal("4242242848428484848484848"),
            db.findUnique(BigDecimal::class, "values (4242242848428484848484848)")
        )
    }

    @Test
    fun `auto-detecting types`() = transactionalTest(db) { db ->
        assertEquals(42, db.findUnique(Any::class, "values (42)"))
        assertEquals("foo", db.findUnique(Any::class, "values ('foo')"))
        assertEquals(true, db.findUnique(Any::class, "values (true)"))
    }

    @Test
    fun `constructor row mapping`() = transactionalTest(db) { db ->
        val departments = db.findAll(Department::class, "select * from (values (1, 'foo'), (2, 'bar')) d")

        assertEquals(2, departments.size)
        assertEquals(1, departments[0].id)
        assertEquals("foo", departments[0].name)
        assertEquals(2, departments[1].id)
        assertEquals("bar", departments[1].name)
    }

    @Test
    fun `findUnique single result`() = transactionalTest(db) { db ->
        assertEquals(42, db.findUnique(Int::class, "values (42)"))
    }

    @Test
    fun `findUnique non-unique result`() = transactionalTest(db) { db ->
        assertFailsWith<NonUniqueResultException> {
            db.findUnique(Int::class, "VALUES (1), (2)")
        }
    }

    @Test
    fun `findUnique empty result`() = transactionalTest(db) { db ->
        assertFailsWith<EmptyResultException> {
            db.findUnique(Int::class, "SELECT * FROM (VALUES (1)) n WHERE FALSE")
        }
    }

    @Test
    fun `findUniqueOrNull single result`() = transactionalTest(db) { db ->
        assertEquals(42, db.findNullableUnique(Int::class, "values (42)"))
    }

    @Test
    fun `findNullableUnique non-unique result`() = transactionalTest(db) { db ->
        assertFailsWith<NonUniqueResultException> {
            db.findNullableUnique(Int::class, "values (1), (2)")
        }
    }

    @Test
    fun `findUniqueOrNull empty result`() = transactionalTest(db) { db ->
        assertNull(db.findUniqueOrNull(Int::class, "select * from (values (1)) n where false"))
    }

    @Test
    fun `findNullableUnique null result`() = transactionalTest(db) { db ->
        assertNull(db.findNullableUnique(Int::class, "values (cast (null as int))"))
    }

    @Test
    fun rowMapper() = transactionalTest(db) { db ->
        val squaringRowMapper = RowMapper { resultSet ->
            val value = resultSet.get<Int>(0)
            value * value
        }

        assertEquals(listOf(1, 4, 9), db.findAll(squaringRowMapper, "values (1), (2), (3)"))
        assertEquals(49, db.findUnique(squaringRowMapper, "values (7)"))
        assertNull(db.findNullableUnique(squaringRowMapper, "select * from (values (1)) n where false"))
    }

    @Test
    fun `custom result processor`() = transactionalTest(db) { db ->
        val rowCounter = object : ResultAggregator<Int> {
            private var rows = 0
            override fun process(row: ResultRow) {
                rows++
            }

            override fun build() = rows
        }

        assertEquals(3, db.executeQuery(rowCounter, "values (1), (2), (3)"))
    }

    class Department(val id: Int, val name: String)
}
