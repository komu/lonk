package dev.komu.lonk.adapter.r2dbc

internal fun translatePlaceholdersForPostgreSQL(sql: String): String {
    val sb = StringBuilder(sql.length + 8)
    var i = 0
    var paramIndex = 1

    while (i < sql.length) {
        val c = sql[i]
        when {
            c == '\'' -> {
                // string literal; handle '' as escaped quote
                sb.append(c)
                i++
                while (i < sql.length) {
                    sb.append(sql[i])
                    if (sql[i] == '\'') {
                        if (i + 1 < sql.length && sql[i + 1] == '\'') {
                            sb.append(sql[++i]) // consume escaped ''
                        } else {
                            i++; break
                        }
                    }
                    i++
                }
                continue
            }

            c == '"' -> {
                // quoted identifier; handle "" as escaped quote, same as '' in string literals
                sb.append(c); i++
                while (i < sql.length) {
                    sb.append(sql[i])
                    if (sql[i] == '"') {
                        if (i + 1 < sql.length && sql[i + 1] == '"') {
                            sb.append(sql[++i]) // consume escaped ""
                        } else {
                            i++; break
                        }
                    }
                    i++
                }
                continue
            }

            c == '-' && i + 1 < sql.length && sql[i + 1] == '-' -> {
                // line comment
                while (i < sql.length && sql[i] != '\n') {
                    sb.append(sql[i]); i++
                }
                continue
            }

            c == '/' && i + 1 < sql.length && sql[i + 1] == '*' -> {
                // block comment, non-nested (Postgres block comments DO nest, unlike most SQL)
                sb.append(sql[i]); sb.append(sql[i + 1]); i += 2
                var depth = 1
                while (i < sql.length && depth > 0) {
                    if (sql[i] == '/' && i + 1 < sql.length && sql[i + 1] == '*') {
                        depth++; sb.append(sql[i]); sb.append(sql[i + 1]); i += 2
                    } else if (sql[i] == '*' && i + 1 < sql.length && sql[i + 1] == '/') {
                        depth--; sb.append(sql[i]); sb.append(sql[i + 1]); i += 2
                    } else {
                        sb.append(sql[i]); i++
                    }
                }
                continue
            }

            c == '?' -> {
                sb.append('$').append(paramIndex++); i++
            }

            else -> {
                sb.append(c); i++
            }
        }
    }
    return sb.toString()
}
