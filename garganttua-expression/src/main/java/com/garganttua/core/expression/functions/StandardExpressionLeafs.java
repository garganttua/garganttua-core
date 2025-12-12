package com.garganttua.core.expression.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.annotations.ExpressionLeaf;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.FixedSupplierBuilder;
import com.garganttua.core.supply.dsl.NullSupplierBuilder;

import jakarta.annotation.Nullable;

/**
 * Type converter functions for expression language.
 *
 * <p>
 * This class provides static converter methods that transform various input types
 * into {@link ISupplier} instances for use in expression evaluation contexts.
 * All methods are annotated with {@link ExpressionLeaf} to be discoverable by
 * the expression framework.
 * </p>
 *
 * <h2>Supported Conversions</h2>
 * <ul>
 * <li>Primitive types: String, Integer, Long, Double, Float, Boolean, Byte, Short, Character</li>
 * <li>Collection types: List, Set, Collection, Map</li>
 * <li>Array types: Object[], primitive arrays</li>
 * <li>Class types: Class<?></li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Convert string
 * ISupplier<String> strSupplier = TypeConverters.String("hello");
 *
 * // Convert integer
 * ISupplier<Integer> intSupplier = TypeConverters.Integer("42");
 *
 * // Convert list
 * ISupplier<List<String>> listSupplier = TypeConverters.List(Arrays.asList("a", "b", "c"));
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 */
public class StandardExpressionLeafs {

    // ========== Primitive Type Converters ==========

    /**
     * Converts a String value to an ISupplier&lt;String&gt;.
     *
     * @param value the string value
     * @return an ISupplier that supplies the string value
     * @throws ExpressionException if value cannot be converted
     */
    @ExpressionLeaf(name = "string", description = "Converts a value to a String supplier")
    public static ISupplier<String> String(@Nullable String value) {
        return new FixedSupplierBuilder<>(value).build();
    }

    /**
     * Converts a String representation to an ISupplier&lt;Integer&gt;.
     *
     * @param value the string representation of an integer
     * @return an ISupplier that supplies the parsed integer value
     * @throws ExpressionException if value cannot be parsed as integer
     */
    @ExpressionLeaf(name = "int", description = "Parses a string to an Integer supplier")
    public static ISupplier<Integer> Integer(@Nullable String value) {
        try {
            return new FixedSupplierBuilder<>(java.lang.Integer.parseInt(value)).build();
        } catch (NumberFormatException e) {
            throw new ExpressionException("Cannot convert '" + value + "' to Integer: " + e.getMessage());
        }
    }

    /**
     * Converts a String representation to an ISupplier&lt;Long&gt;.
     *
     * @param value the string representation of a long
     * @return an ISupplier that supplies the parsed long value
     * @throws ExpressionException if value cannot be parsed as long
     */
    @ExpressionLeaf(name = "long", description = "Parses a string to a Long supplier")
    public static ISupplier<Long> Long(@Nullable String value) {
        try {
            return new FixedSupplierBuilder<>(java.lang.Long.parseLong(value)).build();
        } catch (NumberFormatException e) {
            throw new ExpressionException("Cannot convert '" + value + "' to Long: " + e.getMessage());
        }
    }

    /**
     * Converts a String representation to an ISupplier&lt;Double&gt;.
     *
     * @param value the string representation of a double
     * @return an ISupplier that supplies the parsed double value
     * @throws ExpressionException if value cannot be parsed as double
     */
    @ExpressionLeaf(name = "double", description = "Parses a string to a Double supplier")
    public static ISupplier<Double> Double(@Nullable String value) {
        try {
            return new FixedSupplierBuilder<>(java.lang.Double.parseDouble(value)).build();
        } catch (NumberFormatException e) {
            throw new ExpressionException("Cannot convert '" + value + "' to Double: " + e.getMessage());
        }
    }

    /**
     * Converts a String representation to an ISupplier&lt;Float&gt;.
     *
     * @param value the string representation of a float
     * @return an ISupplier that supplies the parsed float value
     * @throws ExpressionException if value cannot be parsed as float
     */
    @ExpressionLeaf(name = "float", description = "Parses a string to a Float supplier")
    public static ISupplier<Float> Float(@Nullable String value) {
        try {
            return new FixedSupplierBuilder<>(java.lang.Float.parseFloat(value)).build();
        } catch (NumberFormatException e) {
            throw new ExpressionException("Cannot convert '" + value + "' to Float: " + e.getMessage());
        }
    }

    /**
     * Converts a String representation to an ISupplier&lt;Boolean&gt;.
     *
     * @param value the string representation of a boolean ("true" or "false")
     * @return an ISupplier that supplies the parsed boolean value
     */
    @ExpressionLeaf(name = "boolean", description = "Parses a string to a Boolean supplier (true/false)")
    public static ISupplier<Boolean> Boolean(@Nullable String value) {
        return new FixedSupplierBuilder<>(java.lang.Boolean.parseBoolean(value)).build();
    }

    /**
     * Converts a String representation to an ISupplier&lt;Byte&gt;.
     *
     * @param value the string representation of a byte
     * @return an ISupplier that supplies the parsed byte value
     * @throws ExpressionException if value cannot be parsed as byte
     */
    @ExpressionLeaf(name = "byte", description = "Parses a string to a Byte supplier (-128 to 127)")
    public static ISupplier<Byte> Byte(@Nullable String value) {
        try {
            return new FixedSupplierBuilder<>(java.lang.Byte.parseByte(value)).build();
        } catch (NumberFormatException e) {
            throw new ExpressionException("Cannot convert '" + value + "' to Byte: " + e.getMessage());
        }
    }

    /**
     * Converts a String representation to an ISupplier&lt;Short&gt;.
     *
     * @param value the string representation of a short
     * @return an ISupplier that supplies the parsed short value
     * @throws ExpressionException if value cannot be parsed as short
     */
    @ExpressionLeaf(name = "short", description = "Parses a string to a Short supplier")
    public static ISupplier<Short> Short(@Nullable String value) {
        try {
            return new FixedSupplierBuilder<>(java.lang.Short.parseShort(value)).build();
        } catch (NumberFormatException e) {
            throw new ExpressionException("Cannot convert '" + value + "' to Short: " + e.getMessage());
        }
    }

    /**
     * Converts a String representation to an ISupplier&lt;Character&gt;.
     *
     * @param value the string representation of a character (first char is used)
     * @return an ISupplier that supplies the character value
     * @throws ExpressionException if value is empty
     */
    @ExpressionLeaf(name = "char", description = "Extracts first character from string as Character supplier")
    public static ISupplier<Character> Character(@Nullable String value) {
        if (value == null || value.isEmpty()) {
            throw new ExpressionException("Cannot convert empty string to Character");
        }
        return new FixedSupplierBuilder<>(value.charAt(0)).build();
    }

    // ========== Class Type Converter ==========

    /**
     * Converts a fully qualified class name to an ISupplier&lt;Class&lt;?&gt;&gt;.
     *
     * @param className the fully qualified class name
     * @return an ISupplier that supplies the Class object
     * @throws ExpressionException if class cannot be found
     */
    @ExpressionLeaf(name = "class", description = "Loads a class by fully qualified name")
    @SuppressWarnings("unchecked")
    public static ISupplier<Class<?>> Class(@Nullable String className) {
        try {
            Class<?> clazz = java.lang.Class.forName(className);
            return (ISupplier<Class<?>>) (ISupplier<?>) new FixedSupplierBuilder<>(clazz).build();
        } catch (ClassNotFoundException e) {
            throw new ExpressionException("Cannot load class '" + className + "': " + e.getMessage());
        }
    }

    // ========== Collection Type Converters ==========

    /**
     * Converts a varargs array of objects to an ISupplier&lt;List&lt;Object&gt;&gt;.
     *
     * @param values the values to include in the list
     * @return an ISupplier that supplies a list containing the values
     */
    @ExpressionLeaf(name = "list", description = "Creates a mutable List supplier from values")
    @SuppressWarnings("unchecked")
    public static ISupplier<List<Object>> List(@Nullable Object... values) {
        if (values == null) {
            return new FixedSupplierBuilder<List<Object>>(new ArrayList<>()).build();
        }
        List<Object> list = new ArrayList<>(Arrays.asList(values));
        return (ISupplier<List<Object>>) (ISupplier<?>) new FixedSupplierBuilder<>(list).build();
    }

    /**
     * Converts a Collection to an ISupplier&lt;List&lt;T&gt;&gt;.
     *
     * @param <T> the element type
     * @param collection the collection to convert
     * @return an ISupplier that supplies a list containing the collection's elements
     */
    @ExpressionLeaf(name = "listFromCollection", description = "Converts a Collection to a List supplier")
    @SuppressWarnings("unchecked")
    public static <T> ISupplier<List<T>> ListFromCollection(@Nullable Collection<T> collection) {
        if (collection == null) {
            return new FixedSupplierBuilder<List<T>>(new ArrayList<>()).build();
        }
        List<T> list = new ArrayList<>(collection);
        return (ISupplier<List<T>>) (ISupplier<?>) new FixedSupplierBuilder<>(list).build();
    }

    /**
     * Converts a varargs array of objects to an ISupplier&lt;Set&lt;Object&gt;&gt;.
     *
     * @param values the values to include in the set
     * @return an ISupplier that supplies a set containing the values
     */
    @ExpressionLeaf(name = "set", description = "Creates a Set supplier from unique values")
    @SuppressWarnings("unchecked")
    public static ISupplier<Set<Object>> Set(@Nullable Object... values) {
        if (values == null) {
            return new FixedSupplierBuilder<Set<Object>>(new HashSet<>()).build();
        }
        Set<Object> set = new HashSet<>(Arrays.asList(values));
        return (ISupplier<Set<Object>>) (ISupplier<?>) new FixedSupplierBuilder<>(set).build();
    }

    /**
     * Converts a Collection to an ISupplier&lt;Set&lt;T&gt;&gt;.
     *
     * @param <T> the element type
     * @param collection the collection to convert
     * @return an ISupplier that supplies a set containing the collection's elements
     */
    @ExpressionLeaf(name = "setFromCollection", description = "Converts a Collection to a Set supplier")
    @SuppressWarnings("unchecked")
    public static <T> ISupplier<Set<T>> SetFromCollection(@Nullable Collection<T> collection) {
        if (collection == null) {
            return new FixedSupplierBuilder<Set<T>>(new HashSet<>()).build();
        }
        Set<T> set = new HashSet<>(collection);
        return (ISupplier<Set<T>>) (ISupplier<?>) new FixedSupplierBuilder<>(set).build();
    }

    /**
     * Converts a varargs array of objects to an ISupplier&lt;Collection&lt;Object&gt;&gt;.
     *
     * @param values the values to include in the collection
     * @return an ISupplier that supplies a collection containing the values
     */
    @ExpressionLeaf(name = "collection", description = "Creates a generic Collection supplier from values")
    public static ISupplier<Collection<Object>> Collection(@Nullable Object... values) {
        if (values == null) {
            return new FixedSupplierBuilder<Collection<Object>>(new ArrayList<>()).build();
        }
        return new FixedSupplierBuilder<Collection<Object>>(Arrays.asList(values)).build();
    }

    /**
     * Creates an empty ISupplier&lt;Map&lt;String, Object&gt;&gt;.
     *
     * @return an ISupplier that supplies an empty map
     */
    @ExpressionLeaf(name = "map", description = "Creates an empty mutable Map supplier")
    public static ISupplier<Map<String, Object>> Map() {
        return new FixedSupplierBuilder<Map<String, Object>>(new HashMap<>()).build();
    }

    /**
     * Creates an ISupplier&lt;Map&lt;String, Object&gt;&gt; from alternating key-value pairs.
     *
     * @param keyValuePairs alternating keys (String) and values (Object)
     * @return an ISupplier that supplies a map with the specified entries
     * @throws ExpressionException if the number of arguments is odd or if keys are not strings
     */
    @ExpressionLeaf(name = "mapOf", description = "Creates a Map supplier from key-value pairs (key1, val1, key2, val2...)")
    public static ISupplier<Map<String, Object>> MapOf(@Nullable Object... keyValuePairs) {
        if (keyValuePairs == null || keyValuePairs.length == 0) {
            return Map();
        }

        if (keyValuePairs.length % 2 != 0) {
            throw new ExpressionException(
                    "MapOf requires an even number of arguments (key-value pairs), got " + keyValuePairs.length);
        }

        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            Object key = keyValuePairs[i];
            Object value = keyValuePairs[i + 1];

            if (!(key instanceof String)) {
                throw new ExpressionException(
                        "MapOf requires String keys, got " + (key != null ? key.getClass().getSimpleName() : "null")
                                + " at position " + i);
            }

            map.put((String) key, value);
        }

        return new FixedSupplierBuilder<Map<String, Object>>(map).build();
    }

    // ========== Array Type Converters ==========

    /**
     * Converts a varargs array to an ISupplier&lt;Object[]&gt;.
     *
     * @param values the values to include in the array
     * @return an ISupplier that supplies an array containing the values
     */
    @ExpressionLeaf(name = "array", description = "Creates an Object array supplier from values")
    public static ISupplier<Object[]> Array(@Nullable Object... values) {
        if (values == null) {
            return new FixedSupplierBuilder<Object[]>(new Object[0]).build();
        }
        return new FixedSupplierBuilder<>(values).build();
    }

    /**
     * Converts a List to an ISupplier&lt;Object[]&gt;.
     *
     * @param list the list to convert to an array
     * @return an ISupplier that supplies an array containing the list's elements
     */
    @ExpressionLeaf(name = "arrayFromList", description = "Converts a List to an Object array supplier")
    public static ISupplier<Object[]> ArrayFromList(@Nullable List<?> list) {
        if (list == null) {
            return new FixedSupplierBuilder<Object[]>(new Object[0]).build();
        }
        return new FixedSupplierBuilder<>(list.toArray()).build();
    }

    /**
     * Converts a comma-separated string to an ISupplier&lt;String[]&gt;.
     *
     * @param csvString the comma-separated values
     * @return an ISupplier that supplies a string array
     */
    @ExpressionLeaf(name = "stringArray", description = "Parses comma-separated values to a String array supplier")
    public static ISupplier<String[]> StringArray(@Nullable String csvString) {
        if (csvString == null || csvString.isEmpty()) {
            return new FixedSupplierBuilder<String[]>(new String[0]).build();
        }
        String[] parts = csvString.split(",");
        String[] trimmed = Arrays.stream(parts)
                .map(String::trim)
                .toArray(String[]::new);
        return new FixedSupplierBuilder<>(trimmed).build();
    }

    /**
     * Converts a comma-separated string to an ISupplier&lt;int[]&gt;.
     *
     * @param csvString the comma-separated integer values
     * @return an ISupplier that supplies an int array
     * @throws ExpressionException if any value cannot be parsed as integer
     */
    @ExpressionLeaf(name = "intArray", description = "Parses comma-separated integers to an int array supplier")
    public static ISupplier<int[]> IntArray(@Nullable String csvString) {
        if (csvString == null || csvString.isEmpty()) {
            return new FixedSupplierBuilder<int[]>(new int[0]).build();
        }

        try {
            int[] result = Arrays.stream(csvString.split(","))
                    .map(String::trim)
                    .mapToInt(java.lang.Integer::parseInt)
                    .toArray();
            return new FixedSupplierBuilder<>(result).build();
        } catch (NumberFormatException e) {
            throw new ExpressionException("Cannot parse int array from '" + csvString + "': " + e.getMessage());
        }
    }

    /**
     * Converts a comma-separated string to an ISupplier&lt;double[]&gt;.
     *
     * @param csvString the comma-separated double values
     * @return an ISupplier that supplies a double array
     * @throws ExpressionException if any value cannot be parsed as double
     */
    @ExpressionLeaf(name = "doubleArray", description = "Parses comma-separated decimals to a double array supplier")
    public static ISupplier<double[]> DoubleArray(@Nullable String csvString) {
        if (csvString == null || csvString.isEmpty()) {
            return new FixedSupplierBuilder<double[]>(new double[0]).build();
        }

        try {
            double[] result = Arrays.stream(csvString.split(","))
                    .map(String::trim)
                    .mapToDouble(java.lang.Double::parseDouble)
                    .toArray();
            return new FixedSupplierBuilder<>(result).build();
        } catch (NumberFormatException e) {
            throw new ExpressionException("Cannot parse double array from '" + csvString + "': " + e.getMessage());
        }
    }

    // ========== Utility Methods ==========

    /**
     * Converts null to an ISupplier that supplies null.
     *
     * @return an ISupplier that supplies null
     */
    @ExpressionLeaf(name = "null", description = "Creates a null value supplier")
    public static ISupplier<Object> Null() {
        return new NullSupplierBuilder<>(null).build();
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private StandardExpressionLeafs() {
        throw new UnsupportedOperationException("TypeConverters is a utility class and cannot be instantiated");
    }
}
