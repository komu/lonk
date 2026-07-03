package dev.komu.lonk

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

internal class SqlQueryTest {

    @Test
    fun `toString provides meaningful information`() {
        assertEquals("select bar from foo where id=? [42, null]",
            query("select bar from foo where id=?", 42, null).toString()
        )
    }

    @Test
    fun `queries have structural equality`() {
        assertEquals(query("select * from foo"), query("select * from foo"))
        assertEquals(query("select * from foo", 1, 2), query("select * from foo", 1, 2))

        assertNotEquals(query("select * from foo"), query("select * from bar"))
        assertNotEquals(query("select * from foo", 1, 2), query("select * from foo", 1, 3))
    }

    @Test
    fun `hashCode obeys equality`() {
        assertEquals(query("select * from foo").hashCode(), query("select * from foo").hashCode())
        assertEquals(query("select * from foo", 1, 2).hashCode(), query("select * from foo", 1, 2).hashCode())
    }

    @Test
    fun accessors() {
        val query = query("select * from foo", listOf(1, 2, 3))

        assertEquals("select * from foo", query.sql)
        assertEquals(listOf(1, 2, 3), query.arguments)
    }
}
