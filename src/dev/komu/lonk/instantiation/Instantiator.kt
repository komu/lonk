package dev.komu.lonk.instantiation

/**
 * Factory for producing objects from given arguments.
 */
internal fun interface Instantiator<T : Any> {
    fun instantiate(arguments: InstantiatorArguments): T?
}
