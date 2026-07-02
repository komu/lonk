package dev.komu.lonk.conversion

import java.io.ByteArrayOutputStream
import java.sql.Blob
import java.sql.Clob

internal object LobConversions : TypeConversions {
    override fun register(registry: TypeConversionRegistry) {
        registry.registerConversionFromDatabase(Clob::readToString)
        registry.registerConversionFromDatabase(Clob::getCharacterStream)
        registry.registerConversionFromDatabase(Blob::readToByteArray)
        registry.registerConversionFromDatabase(Blob::getBinaryStream)
    }
}

private fun Clob.readToString(): String {
    characterStream.use { reader ->
        val sb = StringBuilder(length().toInt())
        val buf = CharArray(1024)
        var n: Int

        while ((reader.read(buf).also { n = it }) != -1) sb.appendRange(buf, 0, n)
        return sb.toString()
    }
}

private fun Blob.readToByteArray(): ByteArray {
    binaryStream.use { stream ->
        val out = ByteArrayOutputStream(length().toInt())
        val buf = ByteArray(1024)
        var n: Int

        while ((stream.read(buf).also { n = it }) != -1) out.write(buf, 0, n)
        return out.toByteArray()
    }
}
