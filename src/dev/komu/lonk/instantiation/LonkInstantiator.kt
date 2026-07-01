package dev.komu.lonk.instantiation

/**
 * Marks a constructor or a static method as Lonk instantiator. This means that when Lonk tries to
 * instantiate classes of this type, it skips the normal lookup resolution and will always use this constructor.
 * It will not search for other constructors, nor will it set any properties.
 *
 * It is an error to mark multiple constructors/methods as instantiators.
 */
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Target(
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
public annotation class LonkInstantiator
