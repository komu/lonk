package dev.komu.lonk.integration.kotlin

import dev.komu.lonk.DbConnectionProvider
import dev.komu.lonk.result.ResultRow
import dev.komu.lonk.result.ResultRowCollector
import dev.komu.lonk.result.get
import dev.komu.lonk.testutils.DatabaseProvider
import dev.komu.lonk.testutils.DatabaseTest
import dev.komu.lonk.testutils.transactionalTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@DatabaseTest(DatabaseProvider.POSTGRESQL)
internal class DatabaseExtensionsTest(private val db: DbConnectionProvider) {

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
    fun `findUniqueOrNull existing`() = transactionalTest(db) { db ->
        assertEquals(Department(1, "foo"), db.query("select * from (values (1, 'foo')) d").findUnique<Department>())
    }

    @Test
    fun `findUniqueOrNull nonexistent`() = transactionalTest(db) { db ->
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
