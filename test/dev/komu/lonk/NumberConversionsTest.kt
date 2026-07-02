package dev.komu.lonk

import dev.komu.lonk.testutils.DatabaseProvider.POSTGRESQL
import dev.komu.lonk.testutils.DatabaseTest
import dev.komu.lonk.testutils.transactionalTest
import org.junit.jupiter.api.Assertions.assertEquals
import java.math.BigDecimal
import java.math.BigInteger
import java.net.URI
import java.net.URL
import kotlin.test.Test

@DatabaseTest(POSTGRESQL)
internal class NumberConversionsTest(private val db: DbConnectionProvider) {

    @Test
    fun `short conversions`() = transactionalTest(db) { db ->
        assertEquals(42.toShort(), db.findUnique(Short::class, "values (42)"))
        assertEquals(42.toShort(), db.findUnique(Short::class, "values (42)"))
        assertEquals(42.toShort(), db.findUnique(Short::class, "values (cast(42 as bigint))"))
    }

    @Test
    fun `int conversions`() = transactionalTest(db) { db ->
        assertEquals(42, db.findUnique(Int::class, "values (42)"))
        assertEquals(42, db.findUnique<Int>("values (42)"))
        assertEquals(42, db.findUnique(Int::class, "values (cast (42 as bigint))"))
    }

    @Test
    fun `long conversions`() = transactionalTest(db) { db ->
        assertEquals(42L, db.findUnique(Long::class, "values (42)"))
        assertEquals(42L, db.findUniqueLong("values (42)"))
    }

    @Test
    fun `boolean conversions`() = transactionalTest(db) { db ->
        assertEquals(true, db.findUnique(Boolean::class, "select true"))
        assertEquals(true, db.findUnique<Boolean>("select true"))
        assertEquals(false, db.findUnique<Boolean>("select false"))
    }

    @Test
    fun `float conversions`() = transactionalTest(db) { db ->
        assertEquals(42.0f, db.findUnique(Float::class, "values (42)"))
    }

    @Test
    fun `double conversions`() = transactionalTest(db) { db ->
        assertEquals(42.0, db.findUnique(Double::class, "values (42)"))
    }

    @Test
    fun `BigInteger conversions`() = transactionalTest(db) { db ->
        assertEquals(BigInteger.valueOf(42), db.findUnique(BigInteger::class, "values (42)"))
    }

    @Test
    fun `BigDecimal conversions`() = transactionalTest(db) { db ->
        assertEquals(BigDecimal.valueOf(42), db.findUnique(BigDecimal::class, "values (42)"))
    }

    @Test
    fun `number conversions`() = transactionalTest(db) { db ->
        db.update("drop table if exists numbers")
        db.update("create temporary table numbers (short smallint, int int, long bigint, float float, double float, bigint numeric, bigdecimal numeric(100,38))")

        val shortValue = Short.MAX_VALUE
        val intValue = Int.MAX_VALUE
        val longValue = Long.MAX_VALUE
        val floatValue = 442.4204f
        val doubleValue = 42422341233.2424
        val bigIntegerValue = BigInteger("2334593458934593485734985734958734958375984357349857943857")
        val bigDecimalValue = BigDecimal("234239472938472394823.23948723948723948723498237429387423948")

        db.update(
            "insert into numbers (short, int, long, float, double, bigint, bigdecimal) values (?, ?, ?, ?, ?, ?, ?)",
            shortValue, intValue, longValue, floatValue, doubleValue, bigIntegerValue, bigDecimalValue
        )

        val numbers = db.findUnique(Numbers::class, "select * from numbers")

        assertEquals(shortValue, numbers.shortValue)
        assertEquals(intValue, numbers.intValue)
        assertEquals(longValue, numbers.longValue)
        assertEquals(floatValue, numbers.floatValue)
        assertEquals(doubleValue, numbers.doubleValue)
        assertEquals(bigIntegerValue, numbers.bigIntegerValue)
        assertEquals(bigDecimalValue, numbers.bigDecimalValue)
    }

    @Test
    fun `update counts`() = transactionalTest(db) { db ->
        db.update("drop table if exists update_count_test_table")
        db.update("create temporary table update_count_test_table (id int primary key)")

        assertEquals(3, db.update("insert into update_count_test_table (id) values (1), (2), (3)"))

        assertEquals(2, db.update("delete from update_count_test_table where id > 1"))
    }

    @Test
    fun count() = transactionalTest(db) { db ->
        assertEquals(3, db.findUnique<Int>("select count(*) from (values (1), (2), (3)) n"))
    }

    class UrlAndUri(val url: URL, val uri: URI)

    class Numbers(
        val shortValue: Short,
        val intValue: Int,
        val longValue: Long,
        val floatValue: Float,
        val doubleValue: Double,
        val bigIntegerValue: BigInteger,
        val bigDecimalValue: BigDecimal
    )
}
