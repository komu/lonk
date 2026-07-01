package dev.komu.lonk.result

import dev.komu.lonk.adapter.jdbc.getTypes
import dev.komu.lonk.instantiation.Instantiator
import dev.komu.lonk.instantiation.InstantiatorArguments
import dev.komu.lonk.instantiation.InstantiatorProvider
import dev.komu.lonk.instantiation.NamedTypeList
import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

internal class InstantiatorRowMapper<T : Any>(
    private val cl: KClass<T>,
    private val instantiatorProvider: InstantiatorProvider
) :
    RowMapper<T> {

    private var types: NamedTypeList? = null
    private var ctor: Instantiator<T>? = null

    // For performance reasons we reuse the same arguments-array and InstantiatorArguments-object for all rows.
    // This should be fine as long as the instantiators don't hang on to their arguments for too long.
    private var arguments: Array<Any?>? = null

    private var instantiatorArguments: InstantiatorArguments? = null

    override fun mapRow(resultSet: ResultSet): T {
        if (types == null) {
            types = resultSet.metaData.getTypes()
            ctor = instantiatorProvider.findInstantiator(cl, types!!)
            arguments = arrayOfNulls(types!!.size)
            instantiatorArguments = InstantiatorArguments(types!!, arguments!!)
        }

        for (i in arguments!!.indices)
            arguments!![i] = resultSet.getObject(i + 1)

        val value = ctor!!.instantiate(instantiatorArguments!!)
        if (value != null)
            return value
        else
            throw UnexpectedResultException("Expected ${cl.jvmName}, but got null")
    }
}

// FIXME
internal class NullableInstantiatorRowMapper<T : Any>(
    private val cl: KClass<T>,
    private val instantiatorProvider: InstantiatorProvider
) :
    RowMapper<T?> {

    private var types: NamedTypeList? = null
    private var ctor: Instantiator<T>? = null

    // For performance reasons we reuse the same arguments-array and InstantiatorArguments-object for all rows.
    // This should be fine as long as the instantiators don't hang on to their arguments for too long.
    private var arguments: Array<Any?>? = null

    private var instantiatorArguments: InstantiatorArguments? = null

    override fun mapRow(resultSet: ResultSet): T? {
        if (types == null) {
            types = resultSet.metaData.getTypes()
            ctor = instantiatorProvider.findInstantiator(cl, types!!)
            arguments = arrayOfNulls(types!!.size)
            instantiatorArguments = InstantiatorArguments(types!!, arguments!!)
        }

        for (i in arguments!!.indices)
            arguments!![i] = resultSet.getObject(i + 1)

        return ctor!!.instantiate(instantiatorArguments!!)
    }
}
