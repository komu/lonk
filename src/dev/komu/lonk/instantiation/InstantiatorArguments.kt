package dev.komu.lonk.instantiation

/**
 * Contains the arguments of instantiator as well as their names and types.
 */
internal class InstantiatorArguments(val types: NamedTypeList, val values: List<*>) {
    constructor(types: NamedTypeList, values: Array<*>) : this(types, values.asList())

    init {
        require(types.size == values.size) { "got " + types.size + " types, but " + values.size + " values" }
    }

    val size: Int
        get() = types.size

    val singleValue: Any?
        get() {
            check(values.size == 1) { "expected single argument, but got " + values.size }

            return values.first()
        }
}
