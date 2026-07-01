package dev.komu.lonk.testutils

import dev.komu.lonk.DatabaseConnection
import dev.komu.lonk.DatabaseSource
import kotlinx.coroutines.runBlocking

internal fun transactionalTest(db: DatabaseSource, block: suspend (DatabaseConnection) -> Unit) {
    runBlocking {
        db.withConnection { block(it) }
    }
}
