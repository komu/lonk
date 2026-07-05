package dev.komu.lonk

import dev.komu.lonk.conversion.TypeConversions
import dev.komu.lonk.conversion.TypeConversionsConfigurer
import dev.komu.lonk.testutils.DatabaseProvider.POSTGRESQL
import dev.komu.lonk.testutils.DatabaseTest
import dev.komu.lonk.testutils.transactionalTest
import kotlin.test.Test
import kotlin.test.assertEquals

@DatabaseTest(POSTGRESQL, conversions = [DbConnectionReflectiveInstantiationTest.MyConversions::class])
internal class DbConnectionReflectiveInstantiationTest(private val db: DbConnectionProvider) {

    @Test
    fun `constructor binding with conversions`() = transactionalTest(db) { db ->
        val result = db.query("values (42)").findUnique<DtoNeedingConversion>()
        assertEquals(DtoNeedingConversion(IntWrapper(42)), result)
    }

    @Test
    fun `constructor binding with null values`() = transactionalTest(db) { db ->
        val result = db.query("select * from (values (1, cast (null as varchar))) v").findUnique<NullableDto>()
        assertEquals(NullableDto(1, null), result)
    }

    data class DtoNeedingConversion(val value: IntWrapper)

    data class IntWrapper(val value: Int)

    data class NullableDto(val id: Int, val name: String?)

    object MyConversions : TypeConversions {
        override fun registerOn(registry: TypeConversionsConfigurer) {
            registry.registerConversionFromDb(::IntWrapper)
        }
    }
}
