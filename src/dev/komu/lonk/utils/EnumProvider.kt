package dev.komu.lonk.utils

import dev.komu.lonk.InstantiationFailureException
import kotlin.reflect.KClass

/** If the class is an enum, returns an [EnumProvider] for it. */
internal fun <T : Any> KClass<T>.asEnumProviderOrNull(): EnumProvider<T>? =
    if (java.isEnum) EnumProvider(this) else null

internal class EnumProvider<T : Any>(private val cl: KClass<T>) {

    init {
        require(cl.java.isEnum) { "class $cl is not an enum" }
    }

    private val entries = cl.java.enumConstants.asList()

    fun findByName(name: String): T =
        findByKey(name) { (it as Enum<*>).name }

    fun <K> findByKey(key: K, keyFunction: (T) -> K): T =
        entries.find { keyFunction(it) == key }
            ?: throw InstantiationFailureException("could not find enum constant of type ${cl.qualifiedName} for $key")
}
