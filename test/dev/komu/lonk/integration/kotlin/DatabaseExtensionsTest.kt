package dev.komu.lonk.integration.kotlin

import dev.komu.lonk.DatabaseSource
import dev.komu.lonk.result.ResultAggregator
import dev.komu.lonk.result.ResultRow
import dev.komu.lonk.result.get
import dev.komu.lonk.testutils.DatabaseProvider
import dev.komu.lonk.testutils.DatabaseTest
import dev.komu.lonk.testutils.transactionalTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@DatabaseTest(DatabaseProvider.POSTGRESQL)
internal class DatabaseExtensionsTest(private val db: DatabaseSource) {

    @Test
    fun findAll() = transactionalTest(db) { db ->
        assertEquals(
            listOf(Department(1, "foo"), Department(2, "bar")),
            db.findAll<Department>("select * from (values (1, 'foo'), (2, 'bar')) d")
        )
    }

    @Test
    fun `findAll customMapper`() = transactionalTest(db) { db ->
        val result = db.findAll("select * from (values (1, 'foo'), (2, 'bar')) d") { rs ->
            rs.get<Int>(0) to rs.get<String>(1).reversed()
        }

        assertEquals(listOf(1 to "oof", 2 to "rab"), result)
    }

    @Test
    fun findUnique() = transactionalTest(db) { db ->
        assertEquals(Department(1, "foo"), db.findUnique<Department>("select * from (values (1, 'foo')) d"))
    }

    @Test
    fun `findUnique customMapper`() = transactionalTest(db) { db ->
        val result = db.findUnique("select * from (values (1, 'foo')) d") { rs ->
            rs.get<Int>(0) to rs.get<String>(1).reversed()
        }

        assertEquals(1 to "oof", result)
    }

    @Test
    fun `findUniqueOrNull existing`() = transactionalTest(db) { db ->
        assertEquals(Department(1, "foo"), db.findNullableUnique<Department>("select * from (values (1, 'foo')) d"))
    }

    @Test
    fun `findUniqueOrNull nonexistent`() = transactionalTest(db) { db ->
        assertNull(db.findUniqueOrNull<Department>("select * from (values (1, 'foo')) d where 1 = 2"))
    }

    @Test
    fun `findUniqueOrNull customMapper`() = transactionalTest(db) { db ->
        val result = db.findNullableUnique("select * from (values (1, 'foo')) d") { rs ->
            rs.get<Int>(0) to rs.get<String>(1).reversed()
        }

        assertEquals(1 to "oof", result)
    }

    @Test
    fun executeQuery() = transactionalTest(db) { db ->
        val processor = object : ResultAggregator<Pair<List<Int>, List<String>>> {
            private val ints = mutableListOf<Int>()
            private val strs = mutableListOf<String>()

            override fun process(row: ResultRow) {
                ints += row.get<Int>(0)
                strs += row.get<String>(1)

            }

            override fun build() = ints to strs
        }

        val result = db.executeQuery(processor, "select * from (values (1, 'foo'), (2, 'bar')) d")

        assertEquals(listOf(1, 2) to listOf("foo", "bar"), result)
    }

    data class Department(val id: Int, val name: String)
}
