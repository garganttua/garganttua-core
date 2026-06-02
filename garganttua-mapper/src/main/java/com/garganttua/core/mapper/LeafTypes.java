package com.garganttua.core.mapper;

/**
 * Atomic JDK "leaf" types the mapper must NOT recurse into via convention rules.
 *
 * <p>These are passed through as-is (source value reused as destination):
 * primitives &amp; wrappers, {@code String}, JDK numeric / time / math types, and
 * common util types. Field iteration on them is both wrong and unsafe — e.g. a
 * convention pass over {@code String -> String} would try to reflect over
 * {@code String.value} and crash with {@code InaccessibleObjectException} on
 * {@code java.base}.
 */
final class LeafTypes {

    private LeafTypes() {
    }

    static boolean isLeaf(Class<?> type) {
        if (type.isPrimitive()) return true;
        if (type.isEnum()) return true;
        String name = type.getName();
        return switch (name) {
            case "java.lang.String",
                 "java.lang.Boolean",
                 "java.lang.Byte",
                 "java.lang.Short",
                 "java.lang.Character",
                 "java.lang.Integer",
                 "java.lang.Long",
                 "java.lang.Float",
                 "java.lang.Double",
                 "java.lang.Void",
                 "java.lang.Class",
                 "java.lang.Number",
                 "java.math.BigDecimal",
                 "java.math.BigInteger",
                 "java.time.Instant",
                 "java.time.LocalDate",
                 "java.time.LocalDateTime",
                 "java.time.LocalTime",
                 "java.time.OffsetDateTime",
                 "java.time.ZonedDateTime",
                 "java.time.Duration",
                 "java.time.Period",
                 "java.util.Date",
                 "java.util.UUID",
                 "java.util.Locale",
                 "java.util.TimeZone" -> true;
            default -> false;
        };
    }
}
