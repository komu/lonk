package dev.komu.lonk.instantiation

/**
 * Factory for producing objects from given arguments.
 *
 * The instantiator contains all possible conversions needed and applies them to arguments.
 */
internal interface Instantiator<T> {
    fun instantiate(arguments: List<*>): T
}
