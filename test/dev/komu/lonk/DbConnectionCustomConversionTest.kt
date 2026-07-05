package dev.komu.lonk

import dev.komu.lonk.conversion.TypeConversions
import dev.komu.lonk.conversion.TypeConversionsConfigurer
import dev.komu.lonk.testutils.DatabaseProvider.POSTGRESQL
import dev.komu.lonk.testutils.DatabaseTest
import dev.komu.lonk.testutils.transactionalTest
import kotlin.test.Test
import kotlin.test.assertEquals

@DatabaseTest(POSTGRESQL, conversions = [DbConnectionCustomConversionTest.EmailConversions::class])
internal class DbConnectionCustomConversionTest(private val db: DbConnectionProvider) {

    @Test
    fun `custom load conversions`() = transactionalTest(db) { db ->
        assertEquals(
            EmailAddress("user", "example.org"),
            db.query("values ('user@example.org')").findUnique<EmailAddress>()
        )
    }

    @Test
    fun `custom save conversions`() = transactionalTest(db) { db ->
        db.update("drop table if exists custom_save_conversions_test")
        db.update("create temporary table custom_save_conversions_test (email varchar(32))")

        db.update(
            "insert into custom_save_conversions_test (email) values (?)",
            EmailAddress("user", "example.org")
        )

        assertEquals(
            "user@example.org",
            db.query("select email from custom_save_conversions_test").findUnique<String>()
        )
    }

    data class EmailAddress(private val user: String, private val host: String) {

        override fun toString() = "$user@$host"

        companion object {

            private val AT_SIGN = Regex("@")

            fun parse(value: String): EmailAddress {
                val parts = AT_SIGN.split(value)
                if (parts.size == 2)
                    return EmailAddress(parts[0], parts[1])
                throw IllegalArgumentException("invalid address: '$value'")
            }
        }
    }

    internal object EmailConversions : TypeConversions {
        override fun registerOn(registry: TypeConversionsConfigurer) {
            registry.registerConversions(EmailAddress::parse, EmailAddress::toString)
        }
    }
}
