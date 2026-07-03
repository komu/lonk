package dev.komu.lonk

import dev.komu.lonk.adapter.jdbc.JdbcConnectionProvider
import dev.komu.lonk.conversion.registerEnum
import dev.komu.lonk.testutils.DatabaseProvider.POSTGRESQL
import dev.komu.lonk.testutils.DatabaseTest
import dev.komu.lonk.testutils.transactionalTest
import javax.sql.DataSource
import kotlin.test.Test
import kotlin.test.assertEquals

@DatabaseTest(POSTGRESQL)
internal class DbConnectionEnumModeTest(private val ds: DataSource) {

    @Test
    fun `name enum mode`() {
        val db = JdbcConnectionProvider(ds) {
            conversions {
                registerEnum(MyEnum::name)
            }
        }

        transactionalTest(db) { db ->
            db.update("drop table if exists enum_mode_test")
            db.update("create temporary table enum_mode_test (name varchar(10), value varchar(10))")

            db.update("insert into enum_mode_test (name, value) values ('foo', ?)", MyEnum.FOO)

            assertEquals(
                MyEnum.FOO,
                db.query("select value from enum_mode_test where name='foo'").findUnique<MyEnum>()
            )
        }
    }

    @Test
    fun `ordinal enum mode`() {
        val db = JdbcConnectionProvider(ds) {
            conversions {
                registerEnum(MyEnum::ordinal)
            }
        }

        transactionalTest(db) { db ->
            db.update("drop table if exists enum_mode_test")
            db.update("create temporary table enum_mode_test (name varchar(10), value int)")

            db.update("insert into enum_mode_test (name, value) values ('foo', ?)", MyEnum.FOO)

            assertEquals(
                MyEnum.FOO,
                db.query("select value from enum_mode_test where name='foo'").findUnique<MyEnum>()
            )
        }
    }

    enum class MyEnum {
        FOO, BAR, BAZ
    }
}
