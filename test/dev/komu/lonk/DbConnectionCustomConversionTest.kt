package dev.komu.lonk

import dev.komu.lonk.adapter.jdbc.JdbcConnectionProvider
import dev.komu.lonk.conversion.registerConversionFromDatabase
import dev.komu.lonk.conversion.registerConversionToDatabase
import dev.komu.lonk.testutils.DatabaseProvider.POSTGRESQL
import dev.komu.lonk.testutils.DatabaseTest
import dev.komu.lonk.testutils.transactionalTest
import javax.sql.DataSource
import kotlin.test.Test
import kotlin.test.assertEquals

@DatabaseTest(POSTGRESQL)
internal class DbConnectionCustomConversionTest(private val ds: DataSource) {

    @Test
    fun `custom load conversions`() {
        val db = JdbcConnectionProvider(ds) {
            conversions {
                registerConversionFromDatabase(EmailAddress::parse)
            }
        }

        transactionalTest(db) { db ->
            assertEquals(
                EmailAddress("user", "example.org"),
                db.query("values ('user@example.org')").findUnique<EmailAddress>()
            )
        }
    }

    @Test
    fun `custom save conversions`() {
        val db = JdbcConnectionProvider(ds) {
            conversions {
                registerConversionToDatabase(EmailAddress::toString)
            }
        }

        transactionalTest(db) { db ->

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
}
