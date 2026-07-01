package dev.komu.lonk.adapter.jdbc

import java.io.InputStream
import java.io.Reader
import java.sql.PreparedStatement

internal fun PreparedStatement.bindArgument(index: Int, value: Any?) {
    when (value) {
        is InputStream -> setBinaryStream(index, value)
        is Reader -> setCharacterStream(index, value)
        else -> setObject(index, value)
    }
}
