package dev.komu.lonk.conversion

/**
 * A conversion from S into T.
 */
internal class TypeConversion<in S, out T>(private val conversion: (S) -> T) {

    operator fun invoke(value: S): T = conversion(value)

    @Suppress("UNCHECKED_CAST")
    fun <S, T> unsafeCast(): TypeConversion<S, T> =
        this as TypeConversion<S, T>

    @Suppress("UNCHECKED_CAST")
    fun convertUnsafe(value: Any?) =
        value?.let { conversion(it as S) }

    companion object {

        /** Returns identity-conversion, i.e., a conversion that does nothing. */
        fun <T> identity(): TypeConversion<T, T> = TypeConversion { it }
    }
}
