package dev.komu.lonk.testutils

import dev.komu.lonk.DbConnection
import dev.komu.lonk.DbConnectionProvider
import kotlinx.coroutines.runBlocking

internal fun transactionalTest(db: DbConnectionProvider, block: suspend (DbConnection) -> Unit) {
    runBlocking {
        db.withTransaction { block(it) }
    }
}
