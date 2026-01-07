package com.garganttua.core.mutex;

import java.util.Objects;

/**
 * Record representing a qualified mutex name with type class and identifier.
 *
 * <p>
 * {@code MutexName} encapsulates a mutex identifier following the pattern {@code TypeClassName::name},
 * where:
 * </p>
 * <ul>
 *   <li><b>type</b>: The mutex implementation class (e.g., InterruptibleLeaseMutex.class, TestMutex.class)</li>
 *   <li><b>name</b>: The specific mutex identifier within that type</li>
 * </ul>
 *
 * <h2>Format and Validation</h2>
 * <p>
 * The mutex name must follow the pattern {@code TypeClassName::name} where:
 * </p>
 * <ul>
 *   <li>Type must be a non-null Class that implements IMutex</li>
 *   <li>Name must be non-null and non-empty after trimming</li>
 *   <li>The separator is exactly {@code ::} (double colon)</li>
 *   <li>Leading/trailing whitespace in name is trimmed</li>
 * </ul>
 *
 * <h2>Use Cases</h2>
 * <ul>
 *   <li><b>Type Safety</b>: Enforce mutex type at compile time using Class references</li>
 *   <li><b>Factory Selection</b>: Direct mutex creation to appropriate factory based on type</li>
 *   <li><b>Monitoring</b>: Group mutex metrics by implementation type</li>
 *   <li><b>Configuration</b>: Apply different strategies per mutex implementation</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Parse from string (requires fully qualified class name or simple name if resolvable)
 * MutexName name1 = MutexName.fromString("InterruptibleLeaseMutex::user-table");
 * // type: InterruptibleLeaseMutex.class, name: "user-table"
 *
 * MutexName name2 = MutexName.fromString("TestMutex::session-store");
 * // type: TestMutex.class, name: "session-store"
 *
 * // Create directly
 * MutexName name3 = new MutexName(InterruptibleLeaseMutex.class, "file-lock");
 *
 * // Convert to string (uses simple class name)
 * String qualified = name1.toString(); // "InterruptibleLeaseMutex::user-table"
 * }</pre>
 *
 * @param type the mutex implementation class (must not be null and must implement IMutex)
 * @param name the specific mutex identifier (must not be null or empty after trimming)
 * @since 2.0.0-ALPHA01
 * @see IMutex
 * @see IMutexManager
 * @see IMutexFactory
 */
public record MutexName(Class<? extends IMutex> type, String name) {

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

        name = name.trim();

        if (name.isEmpty()) {
            throw new IllegalArgumentException("Mutex name cannot be empty");
        }
    }

    /**
     * Parses a qualified mutex name from a string following the pattern {@code TypeClassName::name}.
     *
     * <p>
     * The input string must contain exactly one {@code ::} separator. The type part must be
     * a valid class name that implements IMutex, and the name part is trimmed and validated.
     * </p>
     *
     * <h2>Valid Examples</h2>
     * <pre>
     * "InterruptibleLeaseMutex::user-table"     → type: InterruptibleLeaseMutex.class, name: "user-table"
     * "TestMutex::session-store"                → type: TestMutex.class, name: "session-store"
     * " TestMutex :: lock "                     → type: TestMutex.class, name: "lock" (whitespace trimmed)
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
     * "UnknownClass::name"       → Class not found
     * </pre>
     *
     * @param qualifiedName the qualified mutex name string (e.g., "TypeClassName::name")
     * @return a {@code MutexName} instance with parsed type and name
     * @throws IllegalArgumentException if the format is invalid, separator is missing,
     *                                  type/name is empty after parsing, or class not found
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
                "Expected format: 'TypeClassName::name', got: '" + qualifiedName + "'"
            );
        }

        // Split by separator
        String[] parts = trimmedInput.split(SEPARATOR, -1); // -1 to preserve empty strings

        // Validate exact format (must have exactly 2 parts)
        if (parts.length != 2) {
            throw new IllegalArgumentException(
                "Invalid mutex name format: expected exactly one '" + SEPARATOR + "' separator. " +
                "Expected format: 'TypeClassName::name', got: '" + qualifiedName + "'"
            );
        }

        String typeStr = parts[0].trim();
        String name = parts[1].trim();

        if (typeStr.isEmpty()) {
            throw new IllegalArgumentException("Mutex type cannot be empty");
        }

        // Try to load the class - first try as-is, then try with common packages
        Class<? extends IMutex> mutexType = loadMutexClass(typeStr);
        return new MutexName(mutexType, name);
    }

    /**
     * Attempts to load a mutex class by name, trying multiple package prefixes.
     *
     * @param className the class name (simple or fully qualified)
     * @return the loaded mutex class
     * @throws IllegalArgumentException if the class cannot be found or doesn't implement IMutex
     */
    @SuppressWarnings("unchecked")
    private static Class<? extends IMutex> loadMutexClass(String className) {
        // List of common packages to try
        String[] packagePrefixes = {
            "",  // Try as-is first (might be fully qualified)
            "com.garganttua.core.mutex.",  // Common mutex package
            "com.garganttua.core.mutex.dsl.fixtures."  // Test fixtures package
        };

        ClassNotFoundException lastException = null;

        for (String prefix : packagePrefixes) {
            String fullClassName = prefix + className;
            try {
                Class<?> clazz = Class.forName(fullClassName);
                if (!IMutex.class.isAssignableFrom(clazz)) {
                    throw new IllegalArgumentException(
                        "Class '" + fullClassName + "' does not implement IMutex"
                    );
                }
                return (Class<? extends IMutex>) clazz;
            } catch (ClassNotFoundException e) {
                lastException = e;
                // Continue trying other prefixes
            }
        }

        // If we get here, none of the attempts worked
        throw new IllegalArgumentException(
            "Mutex type class not found: '" + className + "'. " +
            "Tried packages: com.garganttua.core.mutex, com.garganttua.core.mutex.dsl.fixtures",
            lastException
        );
    }

    /**
     * Returns the qualified mutex name in the format {@code TypeClassName::name}.
     *
     * @return the qualified mutex name string using the simple class name
     */
    @Override
    public String toString() {
        return type.getSimpleName() + SEPARATOR + name;
    }

}
