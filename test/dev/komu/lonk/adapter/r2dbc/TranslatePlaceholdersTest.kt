package dev.komu.lonk.adapter.r2dbc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

internal class TranslatePlaceholdersTest {

    @Nested
    @DisplayName("Basic placeholder translation")
    inner class BasicPlaceholderTranslation {

        @Test
        fun `no placeholders returns input unchanged`() {
            assertEquals("select * from foo", translatePlaceholdersForPostgreSQL("select * from foo"))
        }

        @Test
        fun `empty string returns empty string`() {
            assertEquals("", translatePlaceholdersForPostgreSQL(""))
        }

        @Test
        fun `single placeholder becomes dollar one`() {
            assertEquals("select $1", translatePlaceholdersForPostgreSQL("select ?"))
        }

        @Test
        fun `multiple placeholders numbered sequentially`() {
            assertEquals(
                "insert into foo values ($1, $2, $3)",
                translatePlaceholdersForPostgreSQL("insert into foo values (?, ?, ?)")
            )
        }

        @Test
        fun `placeholder at start of string`() {
            assertEquals("$1 = 1", translatePlaceholdersForPostgreSQL("? = 1"))
        }

        @Test
        fun `placeholder at end of string`() {
            assertEquals("1 = $1", translatePlaceholdersForPostgreSQL("1 = ?"))
        }

        @Test
        fun `only a single placeholder`() {
            assertEquals("$1", translatePlaceholdersForPostgreSQL("?"))
        }

        @Test
        fun `consecutive placeholders`() {
            assertEquals("$1$2$3", translatePlaceholdersForPostgreSQL("???"))
        }

        @Test
        fun `many placeholders numbered correctly beyond single digit`() {
            val input = (1..12).joinToString(",") { "?" }
            val expected = (1..12).joinToString(",") { "$$it" }
            assertEquals(expected, translatePlaceholdersForPostgreSQL(input))
        }
    }

    @Nested
    @DisplayName("String literals")
    inner class StringLiterals {

        @Test
        fun `question mark inside string literal is not translated`() {
            assertEquals("select '?' from foo", translatePlaceholdersForPostgreSQL("select '?' from foo"))
        }

        @Test
        fun `placeholder before and after string literal is translated`() {
            assertEquals(
                "select $1 from foo where x = 'literal' and y = $2",
                translatePlaceholdersForPostgreSQL("select ? from foo where x = 'literal' and y = ?")
            )
        }

        @Test
        fun `escaped single quote inside string literal is preserved`() {
            assertEquals(
                "select 'it''s a ? test' , $1",
                translatePlaceholdersForPostgreSQL("select 'it''s a ? test' , ?")
            )
        }

        @Test
        fun `string literal containing only escaped quotes`() {
            assertEquals("''''", translatePlaceholdersForPostgreSQL("''''"))
        }

        @Test
        fun `empty string literal`() {
            assertEquals("'' $1", translatePlaceholdersForPostgreSQL("'' ?"))
        }

        @Test
        fun `multiple string literals with placeholders interleaved`() {
            assertEquals(
                "'a' $1 'b' $2 'c'",
                translatePlaceholdersForPostgreSQL("'a' ? 'b' ? 'c'")
            )
        }

        @Test
        fun `unterminated string literal consumes rest of input`() {
            // no closing quote; everything after is treated as literal content, ? left untouched
            assertEquals("select 'abc ?", translatePlaceholdersForPostgreSQL("select 'abc ?"))
        }

        @Test
        fun `string literal with newline inside`() {
            assertEquals("'line1\nline2' $1", translatePlaceholdersForPostgreSQL("'line1\nline2' ?"))
        }

        @Test
        fun `string literal containing double quote character`() {
            assertEquals("'a\"b' $1", translatePlaceholdersForPostgreSQL("'a\"b' ?"))
        }

        @Test
        fun `string literal containing dash dash does not start a comment`() {
            assertEquals("'a--b' $1", translatePlaceholdersForPostgreSQL("'a--b' ?"))
        }

        @Test
        fun `string literal containing slash star does not start a block comment`() {
            assertEquals("'a/*b' $1", translatePlaceholdersForPostgreSQL("'a/*b' ?"))
        }

        @Test
        fun `three consecutive quotes then terminator`() {
            // ''' -> escaped quote then unterminated... but here we close it properly: '''' is two escaped quotes forming one literal
            // test odd count: ''' + x + ' => literal containing an escaped quote then x
            assertEquals("'''x' $1", translatePlaceholdersForPostgreSQL("'''x' ?"))
        }
    }

    @Nested
    @DisplayName("Quoted identifiers")
    inner class QuotedIdentifiers {

        @Test
        fun `question mark inside quoted identifier is not translated`() {
            assertEquals(
                "select \"weird?column\" from foo",
                translatePlaceholdersForPostgreSQL("select \"weird?column\" from foo")
            )
        }

        @Test
        fun `placeholder before and after quoted identifier is translated`() {
            assertEquals(
                "select $1 from \"my table\" where x = $2",
                translatePlaceholdersForPostgreSQL("select ? from \"my table\" where x = ?")
            )
        }

        @Test
        fun `empty quoted identifier`() {
            assertEquals("\"\" $1", translatePlaceholdersForPostgreSQL("\"\" ?"))
        }

        @Test
        fun `unterminated quoted identifier consumes rest of input`() {
            assertEquals("select \"abc ?", translatePlaceholdersForPostgreSQL("select \"abc ?"))
        }

        @Test
        fun `quoted identifier containing single quote character`() {
            assertEquals("\"a'b\" $1", translatePlaceholdersForPostgreSQL("\"a'b\" ?"))
        }

        @Test
        fun `quoted identifier containing comment markers`() {
            assertEquals("\"a--b/*c*/\" $1", translatePlaceholdersForPostgreSQL("\"a--b/*c*/\" ?"))
        }

        @Test
        fun `doubled double-quote inside identifier is an escaped quote, not a closing quote`() {
            // "a""b" is a single identifier whose name is: a"b
            assertEquals("\"a\"\"b\" $1", translatePlaceholdersForPostgreSQL("\"a\"\"b\" ?"))
        }

        @Test
        fun `identifier consisting solely of an escaped quote`() {
            // """" is a single-character identifier: "
            assertEquals("\"\"\"\" $1", translatePlaceholdersForPostgreSQL("\"\"\"\" ?"))
        }

        @Test
        fun `question mark between two escaped quotes inside identifier is not translated`() {
            assertEquals(
                "\"a\"\"?\"\"b\" $1",
                translatePlaceholdersForPostgreSQL("\"a\"\"?\"\"b\" ?")
            )
        }

        @Test
        fun `multiple escaped quotes in a row inside identifier`() {
            // """""" -> identifier containing two escaped quotes: ""
            assertEquals("\"\"\"\"\"\" $1", translatePlaceholdersForPostgreSQL("\"\"\"\"\"\" ?"))
        }

        @Test
        fun `identifier with trailing escaped quote then unterminated tail consumes rest of input`() {
            // "a"" starts an identifier, "" is consumed as an escaped quote, then the identifier
            // never actually closes, so everything remaining (including ?) is swallowed verbatim.
            assertEquals("\"a\"\" tail ?", translatePlaceholdersForPostgreSQL("\"a\"\" tail ?"))
        }

        @Test
        fun `two adjacent quoted identifiers each closed normally`() {
            assertEquals(
                "\"a\" \"b\" $1",
                translatePlaceholdersForPostgreSQL("\"a\" \"b\" ?")
            )
        }
    }

    @Nested
    @DisplayName("Line comments")
    inner class LineComments {

        @Test
        fun `question mark inside line comment is not translated`() {
            assertEquals(
                "select 1 -- what about ? here\n",
                translatePlaceholdersForPostgreSQL("select 1 -- what about ? here\n")
            )
        }

        @Test
        fun `placeholder after line comment newline is translated`() {
            assertEquals(
                "select 1 -- comment ?\n and x = $1",
                translatePlaceholdersForPostgreSQL("select 1 -- comment ?\n and x = ?")
            )
        }

        @Test
        fun `line comment at end of input without trailing newline`() {
            assertEquals("select 1 -- comment ?", translatePlaceholdersForPostgreSQL("select 1 -- comment ?"))
        }

        @Test
        fun `single dash is not treated as comment start`() {
            assertEquals("a - $1", translatePlaceholdersForPostgreSQL("a - ?"))
        }

        @Test
        fun `multiple line comments`() {
            assertEquals(
                "$1 -- c1 ?\n$2 -- c2 ?\n$3",
                translatePlaceholdersForPostgreSQL("? -- c1 ?\n? -- c2 ?\n?")
            )
        }
    }

    @Nested
    @DisplayName("Block comments")
    inner class BlockComments {

        @Test
        fun `question mark inside block comment is not translated`() {
            assertEquals(
                "select 1 /* what about ? here */",
                translatePlaceholdersForPostgreSQL("select 1 /* what about ? here */")
            )
        }

        @Test
        fun `placeholder before and after block comment is translated`() {
            assertEquals(
                "$1 /* comment ? */ $2",
                translatePlaceholdersForPostgreSQL("? /* comment ? */ ?")
            )
        }

        @Test
        fun `nested block comments handled correctly postgres style`() {
            assertEquals(
                "/* outer /* inner ? */ still outer ? */ $1",
                translatePlaceholdersForPostgreSQL("/* outer /* inner ? */ still outer ? */ ?")
            )
        }

        @Test
        fun `deeply nested block comments`() {
            val input = "/* a /* b /* c ? */ d ? */ e ? */ ?"
            val expected = "/* a /* b /* c ? */ d ? */ e ? */ $1"
            assertEquals(expected, translatePlaceholdersForPostgreSQL(input))
        }

        @Test
        fun `unterminated block comment consumes rest of input`() {
            assertEquals(
                "select 1 /* comment ? never closes",
                translatePlaceholdersForPostgreSQL("select 1 /* comment ? never closes")
            )
        }

        @Test
        fun `unterminated nested block comment consumes rest of input`() {
            assertEquals(
                "/* outer /* inner never closes ?",
                translatePlaceholdersForPostgreSQL("/* outer /* inner never closes ?")
            )
        }

        @Test
        fun `single slash or single star not treated as comment`() {
            assertEquals("a / b * c $1", translatePlaceholdersForPostgreSQL("a / b * c ?"))
        }

        @Test
        fun `empty block comment`() {
            assertEquals("/**/ $1", translatePlaceholdersForPostgreSQL("/**/ ?"))
        }

        @Test
        fun `block comment immediately followed by another block comment`() {
            assertEquals("/* a */$1/* b */", translatePlaceholdersForPostgreSQL("/* a */?/* b */"))
        }
    }

    @Nested
    @DisplayName("Mixed constructs and realistic queries")
    inner class MixedConstructs {

        @Test
        fun `realistic parametrized insert with comments and literals`() {
            val input = """
                -- insert a new user
                insert into users (id, name, note) /* core insert */
                values (?, ?, 'default note with ? inside')
            """.trimIndent()
            val expected = """
                -- insert a new user
                insert into users (id, name, note) /* core insert */
                values ($1, $2, 'default note with ? inside')
            """.trimIndent()
            assertEquals(expected, translatePlaceholdersForPostgreSQL(input))
        }

        @Test
        fun `query mixing quoted identifier, string literal and comments`() {
            val input = "select \"col?\" from t where name = 'a?b' /* skip ? */ and id = ? -- trailing ?\n and age > ?"
            val expected =
                "select \"col?\" from t where name = 'a?b' /* skip ? */ and id = $1 -- trailing ?\n and age > $2"
            assertEquals(expected, translatePlaceholdersForPostgreSQL(input))
        }

        @Test
        fun `string literal followed immediately by quoted identifier`() {
            assertEquals(
                "'a' \"b\" $1",
                translatePlaceholdersForPostgreSQL("'a' \"b\" ?")
            )
        }

        @Test
        fun `comment immediately followed by string literal containing placeholder`() {
            assertEquals(
                "/* c */'?' $1",
                translatePlaceholdersForPostgreSQL("/* c */'?' ?")
            )
        }

        @Test
        fun `alternating all construct types with many placeholders`() {
            val input = "? '?' \"?\" -- ?\n? /* ? */ ?"
            val expected = "$1 '?' \"?\" -- ?\n$2 /* ? */ $3"
            assertEquals(expected, translatePlaceholdersForPostgreSQL(input))
        }
    }

    @Nested
    @DisplayName("Parameterized sanity checks")
    inner class ParameterizedChecks {

        @ParameterizedTest(name = "placeholder count {0} yields matching $ params")
        @ValueSource(ints = [0, 1, 2, 5, 20, 50])
        fun `placeholder count matches dollar param count`(count: Int) {
            val input = (1..count).joinToString(" ") { "?" }
            val result = translatePlaceholdersForPostgreSQL(input)
            val dollarCount = Regex("""\$\d+""").findAll(result).count()
            assertEquals(count, dollarCount)
        }

        @ParameterizedTest(name = "[{index}] {0} -> {1}")
        @CsvSource(
            "'', ''",
            "'?', '$1'",
            "'no placeholders here', 'no placeholders here'",
            "'a=? and b=?', 'a=$1 and b=$2'",
            delimiter = ','
        )
        fun `table driven basic cases`(input: String, expected: String) {
            assertEquals(expected, translatePlaceholdersForPostgreSQL(input))
        }
    }
}
