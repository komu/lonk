package dev.komu.lonk.adapter.r2dbc

/**
 * Should we automatically translate JDBC-style `?` placeholders in queries into
 * dialect-specific placeholders, or leave the query as it is?
 */
public enum class PlaceholderTranslation {
    /** Keep the queries as they are */
    None,

    /** Translate into PostgreSQL-style $1, $2, ... placeholders */
    PostgreSQL,
}
