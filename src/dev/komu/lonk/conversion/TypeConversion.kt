package dev.komu.lonk.conversion

/**
 * A conversion from S into T.
 */
internal class TypeConversion private constructor(val conversion: (Any?) -> Any?) {

    fun convert(value: Any?): Any? =
        conversion(value)

    fun compose(function: (Any?) -> Any): TypeConversion =
        TypeConversion { function(conversion(it)) }

    companion object {

        fun fromNonNullFunction(function: (Any) -> Any): TypeConversion =
            TypeConversion { value -> if (value != null) function(value) else null }

        /**
         * Returns identity-conversion, i.e., a conversion that does nothing.
         */
        fun identity(): TypeConversion =
            TypeConversion { it }
    }
}
