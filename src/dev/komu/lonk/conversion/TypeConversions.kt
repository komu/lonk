package dev.komu.lonk.conversion

/**
 * A collection of type conversions.
 *
 * Can be implemented to install a set of related type conversions together easily.
 */
public interface TypeConversions {

    /** Called to register the type conversions onto the given registry. */
    public fun registerOn(registry: TypeConversionsConfigurer)
}
