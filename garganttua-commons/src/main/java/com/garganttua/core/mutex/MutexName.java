package com.garganttua.core.mutex;

import java.util.Objects;

/**
 * Record representing a qualified mutex name with type and identifier.
 *
 * <p>
 * {@code MutexName} encapsulates a mutex identifier following the pattern {@code type::name},
 * where:
 * </p>
 * <ul>
 *   <li><b>type</b>: The mutex type or category (e.g., "database", "cache", "resource")</li>
 *   <li><b>name</b>: The specific mutex identifier within that type</li>
 * </ul>
 *
 * <h2>Format and Validation</h2>
 * <p>
 * The mutex name must follow the pattern {@code type::name} where:
 * </p>
 * <ul>
 *   <li>Both type and name must be non-null and non-empty</li>
 *   <li>The separator is exactly {@code ::} (double colon)</li>
 *   <li>Type and name can contain letters, digits, hyphens, and underscores</li>
 *   <li>Leading/trailing whitespace is trimmed</li>
 * </ul>
 *
 * <h2>Use Cases</h2>
 * <ul>
 *   <li><b>Namespacing</b>: Organize mutexes by domain or resource type</li>
 *   <li><b>Routing</b>: Direct mutex operations to appropriate backend (local, Redis, etc.)</li>
 *   <li><b>Monitoring</b>: Group mutex metrics by type</li>
 *   <li><b>Configuration</b>: Apply different strategies per mutex type</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Parse from string
 * MutexName name1 = MutexName.fromString("database::user-table");
 * // type: "database", name: "user-table"
 *
 * MutexName name2 = MutexName.fromString("cache::session-store");
 * // type: "cache", name: "session-store"
 *
 * // Create directly
 * MutexName name3 = new MutexName("resource", "file-lock");
 *
 * // Convert to string
 * String qualified = name1.toString(); // "database::user-table"
 * }</pre>
 *
 * @param type the mutex type or category (must not be null or empty)
 * @param name the specific mutex identifier (must not be null or empty)
 * @since 2.0.0-ALPHA01
 * @see IMutex
 * @see IMutexManager
 * @see IMutexFactory
 */
public record MutexName(String type, String name) {

    /**
     * Separator used between type and name.
     */
    public static final String SEPARATOR = "::";

    /**
     * Compact canonical constructor with validation.
     *
     * @throws IllegalArgumentException if type or name is null or empty after trimming
     */
    public MutexName {
        Objects.requireNonNull(type, "Mutex type cannot be null");
        Objects.requireNonNull(name, "Mutex name cannot be null");

        type = type.trim();
        name = name.trim();

        if (type.isEmpty()) {
            throw new IllegalArgumentException("Mutex type cannot be empty");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Mutex name cannot be empty");
        }
    }

    /**
     * Parses a qualified mutex name from a string following the pattern {@code type::name}.
     *
     * <p>
     * The input string must contain exactly one {@code ::} separator. Both the type
     * and name parts are trimmed of leading/trailing whitespace and validated to be
     * non-empty.
     * </p>
     *
     * <h2>Valid Examples</h2>
     * <pre>
     * "database::user-table"     → type: "database", name: "user-table"
     * "cache::session-store"     → type: "cache", name: "session-store"
     * "resource::file-lock"      → type: "resource", name: "file-lock"
     * " db :: lock "             → type: "db", name: "lock" (whitespace trimmed)
     * </pre>
     *
     * <h2>Invalid Examples</h2>
     * <pre>
     * "invalid"                  → Missing separator
     * "::name"                   → Empty type
     * "type::"                   → Empty name
     * "type:name"                → Wrong separator (single colon)
     * "a::b::c"                  → Too many separators
     * null                       → Null input
     * </pre>
     *
     * @param qualifiedName the qualified mutex name string (e.g., "type::name")
     * @return a {@code MutexName} instance with parsed type and name
     * @throws IllegalArgumentException if the format is invalid, separator is missing,
     *                                  or type/name is empty after parsing
     * @throws NullPointerException if qualifiedName is null
     */
    public static MutexName fromString(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "Qualified mutex name cannot be null");

        String trimmedInput = qualifiedName.trim();
        if (trimmedInput.isEmpty()) {
            throw new IllegalArgumentException("Qualified mutex name cannot be empty");
        }

        // Check if separator exists
        if (!trimmedInput.contains(SEPARATOR)) {
            throw new IllegalArgumentException(
                "Invalid mutex name format: missing '" + SEPARATOR + "' separator. " +
                "Expected format: 'type::name', got: '" + qualifiedName + "'"
            );
        }

        // Split by separator
        String[] parts = trimmedInput.split(SEPARATOR, -1); // -1 to preserve empty strings

        // Validate exact format (must have exactly 2 parts)
        if (parts.length != 2) {
            throw new IllegalArgumentException(
                "Invalid mutex name format: expected exactly one '" + SEPARATOR + "' separator. " +
                "Expected format: 'type::name', got: '" + qualifiedName + "'"
            );
        }

        String type = parts[0].trim();
        String name = parts[1].trim();

        // Validation is handled by the canonical constructor
        return new MutexName(type, name);
    }

    /**
     * Returns the qualified mutex name in the format {@code type::name}.
     *
     * @return the qualified mutex name string
     */
    @Override
    public String toString() {
        return type + SEPARATOR + name;
    }

}
