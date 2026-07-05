package dev.komu.lonk.conversion

import kotlin.reflect.KClass
import kotlin.reflect.cast
import kotlin.reflect.full.isSubclassOf

/**
 * A conversion from S into T.
 */
internal class TypeConversion<S : Any, out T : Any>(
    val source: KClass<S>,
    val target: KClass<out T>,
    private val conversion: (S) -> T,
) {

    operator fun invoke(value: S): T = conversion(value)

    fun <S : Any, T : Any> cast(source: KClass<S>, target: KClass<T>): TypeConversion<S, T> {
        require(source.isSubclassOf(this.source)) { "source $source is not a subclass of ${this.source}" }
        require(this.target.isSubclassOf(target)) { "target ${this.target} is not a subclass of $target." }

        @Suppress("UNCHECKED_CAST")
        return this as TypeConversion<S, T>
    }

    companion object {
        fun <T : Any> identity(cl: KClass<T>): TypeConversion<T, T> = TypeConversion(cl, cl) { it }
    }
}

internal fun <T : Any> Any.convertUnknownWith(converter: TypeConversion<*, T>): T =
    converter(converter.source.cast(this))
