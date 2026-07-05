package dev.komu.lonk

import dev.komu.lonk.conversion.TypeConversions
import dev.komu.lonk.conversion.TypeConversionsConfigurer
import dev.komu.lonk.testutils.DatabaseProvider.POSTGRESQL
import dev.komu.lonk.testutils.DatabaseTest
import dev.komu.lonk.testutils.transactionalTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import kotlin.test.Test
import kotlin.test.assertEquals


internal class DbConnectionEnumModeTest {

    @Nested
    @DisplayName("default enum conversions")
    @DatabaseTest(POSTGRESQL)
    inner class DefaultEnumConversions(private val db: DbConnectionProvider) {

        @Test
        fun `unregistered enums are stored by name and read back by name`() = transactionalTest(db) { db ->
            db.update("drop table if exists default_enum_mode_test")
            db.update("create temporary table default_enum_mode_test (value varchar(10))")

            db.update("insert into default_enum_mode_test (value) values (?)", MyEnum.BAR)

            assertEquals("BAR", db.query("select value from default_enum_mode_test").findUnique<String>())
            assertEquals(MyEnum.BAR, db.query("select value from default_enum_mode_test").findUnique<MyEnum>())
        }
    }

    @Nested
    @DisplayName("name based enums")
    @DatabaseTest(POSTGRESQL, conversions = [NameEnumConversions::class])
    inner class NameBasedEnums(private val db: DbConnectionProvider) {

        @Test
        fun `name enum mode`() = transactionalTest(db) { db ->
            db.update("drop table if exists enum_mode_test")
            db.update("create temporary table enum_mode_test (name varchar(10), value varchar(10))")

            db.update("insert into enum_mode_test (name, value) values ('foo', ?)", MyEnum.FOO)

            assertEquals(
                MyEnum.FOO,
                db.query("select value from enum_mode_test where name='foo'").findUnique<MyEnum>()
            )
        }
    }

    @Nested
    @DisplayName("ordinal based enums")
    @DatabaseTest(POSTGRESQL, conversions = [OrdinalEnumConversions::class])
    inner class OrdinalBasedEnums(private val db: DbConnectionProvider) {

        @Test
        fun `ordinal enum mode`() = transactionalTest(db) { db ->
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

    internal object NameEnumConversions : TypeConversions {
        override fun registerOn(registry: TypeConversionsConfigurer) {
            registry.registerEnum(MyEnum::name)
        }
    }

    internal object OrdinalEnumConversions : TypeConversions {
        override fun registerOn(registry: TypeConversionsConfigurer) {
            registry.registerEnum(MyEnum::ordinal)
        }
    }
}
