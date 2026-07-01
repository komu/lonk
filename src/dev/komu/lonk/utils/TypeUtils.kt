package dev.komu.lonk.utils

import java.lang.reflect.*
import kotlin.reflect.KClass
import kotlin.reflect.KType
import java.lang.reflect.Array as ReflectArray

internal object TypeUtils {

    fun rawType(type: KType): KClass<*> =
        type.classifier as KClass<*>? ?: Any::class

    fun rawType(type: Type): KClass<*> {
        return when (type) {
            is Class<*> -> type.kotlin
            is ParameterizedType -> rawType(type.rawType)
            // We could return one of the bounds, but it won't work if there are multiple bounds.
            // Therefore, just return Any.
            is TypeVariable<*> -> Any::class
            is WildcardType -> rawType(type.upperBounds[0])
            is GenericArrayType -> arrayType(rawType(type.genericComponentType))
            else -> throw IllegalArgumentException("unexpected type: $type")
        }
    }

    fun typeParameter(type: Type): Type {
        if (type is ParameterizedType) return type.actualTypeArguments[0]

        return Any::class.java
    }

    fun isEnum(type: Type): Boolean =
        (type is Class<*>) && type.isEnum

    fun isPrimitive(type: Type): Boolean =
        (type is Class<*>) && type.isPrimitive

    fun arrayType(type: KClass<*>): KClass<*> =
        ReflectArray.newInstance(type.java, 0)::class

    fun genericSuperClass(type: Type): Type? =
        rawType(type).java.getGenericSuperclass()

    fun isAssignable(target: KClass<*>, source: KClass<*>): Boolean =
        rawType(target.java).isAssignableByBoxingFrom(rawType(source.java))

    private fun KClass<*>.isAssignableByBoxingFrom(source: KClass<*>): Boolean =
        this.wrapPrimitive().java.isAssignableFrom(source.wrapPrimitive().java)
}
