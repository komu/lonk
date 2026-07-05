package dev.komu.lonk

import kotlin.test.Test
import kotlin.test.assertEquals

internal class SqlQueryTest {

    @Test
    fun `toString includes sql and arguments`() {
        val query = SqlQuery("select * from foo where id = ?", listOf(42))

        assertEquals("select * from foo where id = ? [42]", query.toString())
    }

    @Test
    fun `toString with multiple arguments`() {
        val query = SqlQuery("select * from foo where id = ? and name = ?", listOf(42, "bar"))

        assertEquals("select * from foo where id = ? and name = ? [42, bar]", query.toString())
    }

    @Test
    fun `toString with no arguments`() {
        val query = SqlQuery("select * from foo", emptyList<Any?>())

        assertEquals("select * from foo []", query.toString())
    }

    @Test
    fun `toString handles null arguments`() {
        val query = SqlQuery("select * from foo where id = ?", listOf(null))

        assertEquals("select * from foo where id = ? [null]", query.toString())
    }
}
