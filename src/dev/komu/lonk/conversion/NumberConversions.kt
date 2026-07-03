package dev.komu.lonk.conversion

import java.math.BigDecimal
import java.math.BigInteger

internal object NumberConversions : TypeConversions {

    override fun register(registry: ConversionsConfigurer) {
        registry.registerConversionFromDatabase(Number::toShort)
        registry.registerConversionFromDatabase(Number::toInt)
        registry.registerConversionFromDatabase(Number::toLong)
        registry.registerConversionFromDatabase(Number::toFloat)
        registry.registerConversionFromDatabase(Number::toDouble)
        registry.registerConversionFromDatabase(Number::convertToBigInteger)
        registry.registerConversionFromDatabase(Number::convertToBigDecimal)
        registry.registerConversionToDatabase<BigInteger> { BigDecimal(it) }
    }
}

private fun Number.convertToBigInteger(): BigInteger = when (this) {
    is BigInteger -> this
    is BigDecimal -> toBigInteger()
    is Int -> toBigInteger()
    is Long -> toBigInteger()
    else -> BigInteger(this.toString())
}

private fun Number.convertToBigDecimal(): BigDecimal = when (this) {
    is BigDecimal -> this
    is BigInteger -> toBigDecimal()
    is Int -> toBigDecimal()
    is Long -> toBigDecimal()
    else -> BigDecimal(this.toString())
}
