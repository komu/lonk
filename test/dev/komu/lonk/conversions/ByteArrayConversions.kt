package dev.komu.lonk.conversions

import dev.komu.lonk.conversion.TypeConversions
import dev.komu.lonk.conversion.TypeConversionsConfigurer
import java.nio.ByteBuffer

internal object ByteArrayConversions : TypeConversions {
    override fun registerOn(registry: TypeConversionsConfigurer) {
        registry.registerConversionFromDb(ByteBuffer::toByteArray)
    }
}

private fun ByteBuffer.toByteArray(): ByteArray =
    ByteArray(remaining()).also { get(it) }
