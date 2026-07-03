package dev.komu.lonk

import dev.komu.lonk.testutils.DatabaseProvider.POSTGRESQL
import dev.komu.lonk.testutils.DatabaseTest
import dev.komu.lonk.testutils.transactionalTest
import kotlinx.coroutines.*
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

@DatabaseTest(POSTGRESQL)
internal class DatabaseCancellationTest(private val provider: DbConnectionProvider) {

    @Test
    fun `findUnique cancellation stops the query on the server, not just locally`() {
        val elapsed = measureTime {
            runBlocking {
                provider.withConnection { db ->
                    // pg_sleep(10) blocks server-side for 10s. If cancellation only abandoned the
                    // coroutine without calling PreparedStatement.cancel(), this test would still
                    // "pass" from Kotlin's perspective while the server kept running the query —
                    // the elapsed-time assertion is what catches that.

                    coroutineScope {
                        val job = launch {
                            db.sleepInDatabase()
                        }
                        delay(300.milliseconds) // give the query time to actually start executing server-side
                        job.cancelAndJoin()
                    }
                }
            }
        }
        assertTrue(elapsed < 5.seconds, "expected cancellation well before the 10s pg_sleep, took ${elapsed}ms")
    }

    @Test
    fun `findUnique cancellation propagates as CancellationException`(): Unit = runBlocking {
        var caught: Throwable? = null

        coroutineScope {
            val job = launch {
                try {
                    provider.withConnection { db ->
                        db.sleepInDatabase()
                    }
                } catch (e: Throwable) {
                    caught = e
                }
            }
            delay(300.milliseconds)
            job.cancelAndJoin()
        }

        assertIs<CancellationException>(caught)
    }

    @Test
    fun `connection remains usable after a cancelled query`() = transactionalTest(provider) { db ->
        coroutineScope {
            val job = launch {
                db.sleepInDatabase()
            }
            delay(300.milliseconds)
            job.cancelAndJoin()
        }

        db.rollback()

        // If ps.cancel() left the underlying connection/session in a bad state
        // (e.g. driver didn't fully drain the cancelled query before the next one),
        // this would fail or hang instead of returning cleanly.
        assertEquals(42, db.findUnique<Int>("values (42)"))
    }

    @Test
    fun `non-cancelled concurrent query on a different connection is unaffected`() = runBlocking {
        // Guards against a bad cancel() implementation calling `cancel()` on the wrong
        // statement/connection, or a shared dispatcher issue causing cross-talk.
        coroutineScope {
            val victim = launch {
                provider.withTransaction { db ->
                    db.sleepInDatabase()
                }
            }
            val bystander = async {
                provider.withTransaction { other ->
                    other.findUnique(Int::class, "values (7)")
                }
            }
            delay(300.milliseconds)
            victim.cancel()
            assertEquals(7, bystander.await())
            victim.join()
        }
    }

    private suspend fun DbConnection.sleepInDatabase() {
        val _ = findUnique(Int::class, "select pg_sleep(10)::text, 1")
    }
}
