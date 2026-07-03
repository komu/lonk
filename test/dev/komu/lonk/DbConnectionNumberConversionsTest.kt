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
internal class DbConnectionNumberConversionsTest(private val db: DbConnectionProvider) {

    @Test
    fun `short conversions`() = transactionalTest(db) { db ->
        assertEquals(42.toShort(), db.query("values (42)").findUnique<Short>())
        assertEquals(42.toShort(), db.query("values (42)").findUnique<Short>())
        assertEquals(42.toShort(), db.query("values (cast(42 as bigint))").findUnique<Short>())
    }

    @Test
    fun `int conversions`() = transactionalTest(db) { db ->
        assertEquals(42, db.query("values (42)").findUnique<Int>())
        assertEquals(42, db.query("values (42)").findUnique<Int>())
        assertEquals(42, db.query("values (cast (42 as bigint))").findUnique<Int>())
    }

    @Test
    fun `long conversions`() = transactionalTest(db) { db ->
        assertEquals(42L, db.query("values (42)").findUnique<Long>())
        assertEquals(42L, db.query("values (42)").findUnique<Long>())
    }

    @Test
    fun `boolean conversions`() = transactionalTest(db) { db ->
        assertEquals(true, db.query("select true").findUnique<Boolean>())
        assertEquals(true, db.query("select true").findUnique<Boolean>())
        assertEquals(false, db.query("select false").findUnique<Boolean>())
    }

    @Test
    fun `float conversions`() = transactionalTest(db) { db ->
        assertEquals(42.0f, db.query("values (42)").findUnique<Float>())
    }

    @Test
    fun `double conversions`() = transactionalTest(db) { db ->
        assertEquals(42.0, db.query("values (42)").findUnique<Double>())
    }

    @Test
    fun `BigInteger conversions`() = transactionalTest(db) { db ->
        assertEquals(BigInteger.valueOf(42), db.query("values (42)").findUnique<BigInteger>())
    }

    @Test
    fun `BigDecimal conversions`() = transactionalTest(db) { db ->
        assertEquals(BigDecimal.valueOf(42), db.query("values (42)").findUnique<BigDecimal>())
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

        val numbers = db.query("select * from numbers").findUnique<Numbers>()

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
        assertEquals(3, db.query("select count(*) from (values (1), (2), (3)) n").findUnique<Int>())
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
