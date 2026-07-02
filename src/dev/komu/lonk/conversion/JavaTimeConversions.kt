package dev.komu.lonk.conversion

import java.time.OffsetDateTime
import java.time.ZoneOffset

internal object JavaTimeConversions : TypeConversions {
    override fun register(registry: TypeConversionRegistry) {
        registry.registerConversions(java.sql.Time::toLocalTime, java.sql.Time::valueOf)
        registry.registerConversions(java.sql.Date::toLocalDate, java.sql.Date::valueOf)
        registry.registerConversions(java.sql.Timestamp::toInstant, java.sql.Timestamp::from)
        registry.registerConversions(java.sql.Timestamp::toLocalDateTime, java.sql.Timestamp::valueOf)
    }
}

internal object JavaTimeWithZoneConversions : TypeConversions {

    override fun register(registry: TypeConversionRegistry) {
        registry.registerConversions(OffsetDateTime::toInstant) { OffsetDateTime.ofInstant(it, ZoneOffset.UTC) }
    }
}
