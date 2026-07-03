package dev.komu.lonk.integration.java8

import dev.komu.lonk.DbConnectionProvider
import dev.komu.lonk.testutils.DatabaseProvider
import dev.komu.lonk.testutils.DatabaseTest
import dev.komu.lonk.testutils.transactionalTest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals

@DatabaseTest(DatabaseProvider.POSTGRESQL)
internal class JavaTimeIntegrationTest(private val db: DbConnectionProvider) {

    @Test
    fun `fetch LocalDateTime`() = transactionalTest(db) { db ->
        assertEquals(
            LocalDateTime.of(2012, 10, 9, 11, 29, 25),
            db.query("VALUES (cast('2012-10-09 11:29:25' AS TIMESTAMP))").findUnique<LocalDateTime>()
        )
    }

    @Test
    fun `fetch Instant`() = transactionalTest(db) { db ->
        val time = Instant.ofEpochMilli(1295000000000L)
        assertEquals(
            time,
            db.query("VALUES (cast('2011-01-14 10:13:20+00' AS TIMESTAMP WITH TIME ZONE))").findUnique<Instant>()
        )
    }

    @Test
    fun `store Instant`() = transactionalTest(db) { db ->
        db.update("DROP TABLE IF EXISTS instant_test")
        db.update("CREATE TABLE instant_test (timestamp TIMESTAMP WITH TIME ZONE)")

        val instant = Instant.now().truncatedTo(ChronoUnit.MILLIS)

        db.update("INSERT INTO instant_test (timestamp) VALUES (?)", instant)

        assertEquals(instant, db.query("SELECT timestamp FROM instant_test").findUnique<Instant>())
    }

    @Test
    fun `fetch LocalDates`() = transactionalTest(db) { db ->
        assertEquals(
            LocalDate.of(2012, 10, 9),
            db.query("VALUES (cast('2012-10-09' AS DATE))").findUnique<LocalDate>()
        )
    }

    @Test
    fun `fetch LocalTime`() = transactionalTest(db) { db ->
        assertEquals(
            LocalTime.of(11, 29, 25),
            db.query("VALUES (cast('11:29:25' AS TIME))").findUnique<LocalTime>()
        )
    }

    @Test
    fun `LocalDates with time-zone problems`() = transactionalTest(db) { db ->
        assertEquals(
            LocalDate.of(2012, 10, 9),
            db.query("VALUES (cast('2012-10-09' AS DATE))").findUnique<LocalDate>()
        )
    }

    @Test
    fun `time types as parameters`() = transactionalTest(db) { db ->
        val container = db
            .query("VALUES (cast('2012-10-09 11:29:25' AS TIMESTAMP), cast('2012-10-09' AS DATE), cast('11:29:25' AS TIME))")
            .findUnique<DateContainer>()

        assertEquals(LocalDateTime.of(2012, 10, 9, 11, 29, 25), container.dateTime)
        assertEquals(LocalDate.of(2012, 10, 9), container.date)
        assertEquals(LocalTime.of(11, 29, 25), container.time)
    }

    @Test
    fun `save java time types`() = transactionalTest(db) { db ->
        db.update("DROP TABLE IF EXISTS date_test")
        db.update("CREATE TABLE date_test (timestamp TIMESTAMP, date DATE, time TIME)")

        val dateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)
        val date = LocalDate.now()
        val time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS)

        db.update("INSERT INTO date_test (timestamp, date, time) VALUES (?, ?, ?)", dateTime, date, time)

        assertEquals(dateTime, db.query("SELECT timestamp FROM date_test").findUnique<LocalDateTime>())
        assertEquals(date, db.query("SELECT date FROM date_test").findUnique<LocalDate>())
        assertEquals(time, db.query("SELECT time FROM date_test").findUnique<LocalTime>())
    }

    class DateContainer(val dateTime: LocalDateTime, val date: LocalDate, val time: LocalTime)
}
