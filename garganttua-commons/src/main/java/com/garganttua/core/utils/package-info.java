/**
 * Common utility classes and helper methods used across Garganttua modules.
 *
 * <h2>Overview</h2>
 * <p>
 * This package contains general-purpose utility classes for collection operations,
 * object copying, and other cross-cutting concerns used throughout the framework.
 * </p>
 *
 * <h2>Core Utilities</h2>
 * <ul>
 *   <li><b>OrderedMap</b> - LinkedHashMap with positional insertion capabilities</li>
 *   <li><b>Copyable</b> - Interface for objects that can be copied</li>
 *   <li><b>CopyException</b> - Exception for copy operation failures</li>
 * </ul>
 *
 * <h2>OrderedMap</h2>
 * <p>
 * {@link com.garganttua.core.utils.OrderedMap} extends the standard LinkedHashMap functionality
 * with the ability to insert elements at specific positions relative to existing keys using
 * {@link com.garganttua.core.runtime.Position} enum (BEFORE or AFTER).
 * </p>
 *
 * <h3>Basic Usage</h3>
 * <pre>{@code
 * OrderedMap<String, String> map = new OrderedMap<>();
 *
 * // Standard put operations
 * map.put("a", "Alpha");
 * map.put("c", "Charlie");
 *
 * // Insert "b" BEFORE "c"
 * map.putAt("b", "Bravo", "c", Position.BEFORE);
 * // Result order: a, b, c
 *
 * // Insert "d" AFTER "a"
 * map.putAt("d", "Delta", "a", Position.AFTER);
 * // Result order: a, d, b, c
 * }</pre>
 *
 * <h3>Edge Cases</h3>
 * <pre>{@code
 * // If reference key doesn't exist, element is added at the end
 * map.putAt("e", "Echo", "nonexistent", Position.BEFORE);
 *
 * // Duplicate keys throw IllegalArgumentException
 * map.put("a", "Alpha");
 * map.put("a", "Another Alpha"); // Throws: "Key already exists: a"
 *
 * // Convert to standard Map
 * Map<String, String> standardMap = map.asMap();
 * }</pre>
 *
 * <h3>Collection Operations</h3>
 * <pre>{@code
 * // Check for keys and values
 * boolean hasKey = map.containsKey("a");        // true
 * boolean hasValue = map.containsValue("Alpha"); // true
 *
 * // Remove elements
 * String removed = map.remove("a");              // Returns "Alpha"
 *
 * // Add multiple entries
 * OrderedMap<String, String> other = new OrderedMap<>();
 * other.put("x", "X-ray");
 * other.put("y", "Yankee");
 * map.putAll(other);
 *
 * // Size and emptiness checks
 * int size = map.size();
 * boolean empty = map.isEmpty();
 *
 * // Clear all entries
 * map.clear();
 * }</pre>
 *
 * <h2>Design Principles</h2>
 * <ul>
 *   <li>Maintain insertion order with flexible positioning</li>
 *   <li>Prevent duplicate keys with clear error messages</li>
 *   <li>Graceful handling of invalid reference keys</li>
 *   <li>Standard Map interface compatibility via asMap()</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.utils.OrderedMap
 * @see com.garganttua.core.runtime.Position
 */
package com.garganttua.core.utils;
