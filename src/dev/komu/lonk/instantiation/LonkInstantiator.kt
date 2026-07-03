package dev.komu.lonk.instantiation

/**
 * Marks a constructor or a static method as Lonk instantiator. This means that when Lonk tries to
 * instantiate classes of this type, it skips the normal lookup resolution and will always use this constructor.
 * It will not search for other constructors.
 *
 * It is an error to mark multiple constructors/methods as instantiators.
 */
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Target(AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION)
public annotation class LonkInstantiator
