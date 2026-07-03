package dev.komu.lonk.instantiation

import dev.komu.lonk.InstantiationFailureException
import dev.komu.lonk.conversion.DefaultTypeConversionRegistry
import dev.komu.lonk.conversion.TypeConversion
import dev.komu.lonk.utils.TypeUtils.isAssignable
import dev.komu.lonk.utils.TypeUtils.isEnum
import dev.komu.lonk.utils.TypeUtils.rawType
import java.lang.reflect.Modifier.isPublic
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.jvm.jvmName

/**
 * Provides [Instantiator]s for classes.
 */
internal class InstantiatorProvider(private val typeConversionRegistry: DefaultTypeConversionRegistry) {

    fun valueToDatabase(value: Any?): Any? {
        if (value == null) return null

        val conversion = typeConversionRegistry.findConversionToDb(value::class)
        return when {
            conversion != null -> conversion(value)
            value is Enum<*> -> value.name
            else -> value
        }
    }

    fun <T : Any> findInstantiator(type: KClass<T>, types: List<KClass<*>>): Instantiator<T> {
        // First, check if we have an immediate conversion registered. If so, we'll just use that.
        if (types.size == 1) {
            val conversion = findConversionFromDbValue(types.single(), type)
            if (conversion != null)
                return ImmediateSingleValueInstantiator(type, conversion)
        }

        if (types.size == 1 && type.isValue)
            return ImmediateSingleValueInstantiator(type, TypeConversion.identity)

        val instantiator = findExplicitInstantiatorFor(type, types)
        if (instantiator != null)
            return instantiator

        if (!isPublic(type.java.modifiers))
            throw InstantiationFailureException("$type can't be instantiated reflectively because it is not public or missing a @LonkInstantiator-annotation");

        return type.constructors
            .firstNotNullOfOrNull { implicitInstantiatorFrom(it, types) }
            ?: throw InstantiationFailureException("could not find a way to instantiate $type with parameters $types")
    }

    private fun <T : Any> findExplicitInstantiatorFor(cl: KClass<T>, types: List<KClass<*>>): Instantiator<T>? {
        val constructors = cl.constructors.filter { it.hasAnnotation<LonkInstantiator>() }
        val methods =
            cl.companionObject?.declaredMemberFunctions?.filter { it.hasAnnotation<LonkInstantiator>() }.orEmpty()

        val count = constructors.size + methods.size
        return when {
            count > 1 ->
                throw InstantiationFailureException("only one constructor/method of $cl can be marked with @LonkInstantiator. Found $count")

            constructors.size == 1 -> {
                val ctor = constructors.first()
                val parameterTypes = ctor.parameters.map { rawType(it.type) }

                resolveConversions(types, parameterTypes)
                    ?.let { ConstructorInstantiator(ctor, it) }
                    ?: throw InstantiationFailureException("could not find a way to instantiate ${cl.jvmName} with parameters $types")
            }

            methods.size == 1 -> {
                val method = methods.first()
                if (method.returnType.classifier != cl)
                    throw InstantiationFailureException("Instantiator method ${method.name} does not return $cl but ${method.returnType}")

                val parameterTypes = method.parameters.map { rawType(it.type) }
                    .drop(1) // first parameter is the companion object

                resolveConversions(types, parameterTypes)
                    ?.let { CompanionFunctionInstantiator(cl, cl.companionObjectInstance!!, method, it) }
                    ?: throw InstantiationFailureException("could not find a way to instantiate ${cl.jvmName} with parameters $types")
            }

            else
                -> null
        }
    }

    /**
     * Returns an instantiator that uses given constructor and given types to create instances,
     * or empty if there are no conversions that can be made to instantiate the type.
     */
    private fun <T : Any> implicitInstantiatorFrom(ctor: KFunction<T>, types: List<KClass<*>>): Instantiator<T>? {
        if (ctor.visibility == KVisibility.PRIVATE || ctor.visibility == KVisibility.PROTECTED)
            return null

        if (ctor.parameters.size != types.size)
            return null

        val conversions = resolveConversions(types, ctor.parameters.map { rawType(it.type) })
            ?: return null

        return ConstructorInstantiator(ctor, conversions)
    }

    /**
     * Returns the list of conversions that need to be performed to convert
     * from sourceTypes to targetTypes, or `null` if conversions can't be done.
     */
    private fun resolveConversions(sourceTypes: List<KClass<*>>, targetTypes: List<KClass<*>>): List<TypeConversion>? {
        require(targetTypes.size == sourceTypes.size)

        return List(targetTypes.size) { i ->
            findConversionFromDbValue(sourceTypes[i], targetTypes[i]) ?: return null
        }
    }

    /**
     * Returns conversion for converting value of source to target, or returns null if there's no such conversion.
     */
    private fun findConversionFromDbValue(source: KClass<*>, target: KClass<*>): TypeConversion? {
        if (isAssignable(target, source))
            return TypeConversion.identity

        return typeConversionRegistry.findConversionFromDbValue(source, target)
            ?: findEnumConversion(target)
    }

    private fun findEnumConversion(target: KClass<*>): TypeConversion? {
        if (isEnum(target.java)) {
            val cl = rawType(target.java).java.asSubclass(Enum::class.java)
            return TypeConversion.fromNonNullFunction { value ->
                java.lang.Enum.valueOf(
                    cl,
                    value.toString()
                )
            }
        }

        return null
    }
}
