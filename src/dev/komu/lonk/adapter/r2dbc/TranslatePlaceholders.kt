package dev.komu.lonk.adapter.r2dbc

internal fun translatePlaceholdersForPostgreSQL(sql: String): String {
    val translator = PlaceholderTranslator(sql)
    translator.translate()
    return translator.build()
}

private class PlaceholderTranslator(private val sql: String) {

    private var i = 0
    private var params = 0
    private val result = StringBuilder(sql.length + 8)

    private fun readNext(): Char =
        sql[i++]

    private val hasMore: Boolean
        get() = i < sql.length

    fun build() = result.toString()

    fun translate() {
        while (hasMore) {
            when (sql[i]) {
                '?' ->
                    processParam()

                '\'', '"' ->
                    processStringLiteral()

                '-' if consumeIfLookingAt("--") ->
                    processLineComment()

                '/' if consumeIfLookingAt("/*") ->
                    processBlockComment()

                else ->
                    result.append(readNext())
            }
        }
    }

    private fun processParam() {
        i++ // consume '?'
        result.append('$').append(++params)
    }

    @IgnorableReturnValue
    private fun copyNext(): Char {
        val c = sql[i++]
        result.append(c)
        return c
    }

    private fun processBlockComment() {
        result.append("/*")

        var depth = 1

        while (hasMore && depth > 0) {
            if (consumeIfLookingAt("/*")) {
                result.append("/*")
                depth++
            } else if (consumeIfLookingAt("*/")) {
                result.append("*/")
                depth--
            } else {
                copyNext()
            }
        }
    }

    private fun consumeIfLookingAt(str: String) =
        if (sql.startsWith(str, i)) {
            i += str.length
            true
        } else {
            false
        }

    private fun processLineComment() {
        result.append("--")
        while (hasMore) {
            val c = copyNext()
            if (c == '\n')
                break
        }
    }

    private fun processStringLiteral() {
        val quote = sql[i++] // remember the kind of quote that started this literal

        result.append(quote)

        while (hasMore) {
            val c = copyNext()

            if (c == quote) {
                // We just saw a quote, there are two possibilities:
                //   1. there are two successive quotes: we just copy them and continue
                //   2. there's no quote, we are at the end of string and break
                if (sql.getOrNull(i) == quote)
                    copyNext()
                else
                    break
            }
        }
    }
}
