package dev.komu.lonk.instantiation

import kotlin.reflect.KClass

/**
 * Represents a named list of types, e.g. the result types of SQL-query.
 */
internal class NamedTypeList private constructor(val names: List<String>, val types: List<KClass<*>>) {

    init {
        require(names.size == types.size)
    }

    val size: Int
        get() = types.size

    fun getName(index: Int): String = names[index]

    fun getType(index: Int): KClass<*> = types[index]

    override fun toString(): String {
        val size = types.size

        val sb = StringBuilder(10 + size * 30)

        sb.append('[')

        for (i in names.indices) {
            if (i != 0) sb.append(", ")

            sb.append(names[i]).append(": ").append(types[i].qualifiedName)
        }

        sb.append(']')

        return sb.toString()
    }

    /**
     * Builder for [NamedTypeList]s.
     */
    class Builder internal constructor(size: Int) {
        private var built = false

        private val names = ArrayList<String>(size)

        private val types = ArrayList<KClass<*>>(size)

        fun add(name: String, type: KClass<*>) {
            check(!built) { "can't add items to builder that has been built" }
            names.add(name)
            types.add(type)
        }

        fun build(): NamedTypeList {
            built = true
            return NamedTypeList(names, types)
        }
    }

    companion object {

        val empty = Builder(0).build()

        fun build(vararg items: Pair<String, KClass<*>>): NamedTypeList {
            val builder = Builder(items.size)
            for (item in items)
                builder.add(item.first, item.second)
            return builder.build()
        }

        fun builder(size: Int): Builder =
            Builder(size)
    }
}
