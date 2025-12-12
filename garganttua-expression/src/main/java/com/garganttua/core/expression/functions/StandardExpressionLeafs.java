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
    @ExpressionLeaf(name = "string", description = "Converts a value to a String")
    public static String String(@Nullable String value) {
        return value;
    }

    /**
     * Converts a String representation to an ISupplier&lt;Integer&gt;.
     *
     * @param value the string representation of an integer
     * @return an ISupplier that supplies the parsed integer value
     * @throws ExpressionException if value cannot be parsed as integer
     */
    @ExpressionLeaf(name = "int", description = "Parses a string to an Integer")
    public static Integer Integer(@Nullable String value) {
        try {
            return java.lang.Integer.parseInt(value);
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
    @ExpressionLeaf(name = "long", description = "Parses a string to a Long")
    public static Long Long(@Nullable String value) {
        try {
            return java.lang.Long.parseLong(value);
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
    @ExpressionLeaf(name = "double", description = "Parses a string to a Double")
    public static Double Double(@Nullable String value) {
        try {
            return java.lang.Double.parseDouble(value);
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
    @ExpressionLeaf(name = "float", description = "Parses a string to a Float")
    public static Float Float(@Nullable String value) {
        try {
            return java.lang.Float.parseFloat(value);
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
    @ExpressionLeaf(name = "boolean", description = "Parses a string to a Boolean (true/false)")
    public static Boolean Boolean(@Nullable String value) {
        return java.lang.Boolean.parseBoolean(value);
    }

    /**
     * Converts a String representation to an ISupplier&lt;Byte&gt;.
     *
     * @param value the string representation of a byte
     * @return an ISupplier that supplies the parsed byte value
     * @throws ExpressionException if value cannot be parsed as byte
     */
    @ExpressionLeaf(name = "byte", description = "Parses a string to a Byte (-128 to 127)")
    public static Byte Byte(@Nullable String value) {
        try {
            return java.lang.Byte.parseByte(value);
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
    @ExpressionLeaf(name = "short", description = "Parses a string to a Short")
    public static Short Short(@Nullable String value) {
        try {
            return java.lang.Short.parseShort(value);
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
    @ExpressionLeaf(name = "char", description = "Extracts first character from string as Character")
    public static Character Character(@Nullable String value) {
        if (value == null || value.isEmpty()) {
            throw new ExpressionException("Cannot convert empty string to Character");
        }
        return value.charAt(0);
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
    public static Class<?> Class(@Nullable String className) {
        try {
            return java.lang.Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new ExpressionException("Cannot load class '" + className + "': " + e.getMessage());
        }
    }

    // ========== Collection Type Converters ==========

    /**
     * Private constructor to prevent instantiation.
     */
    private StandardExpressionLeafs() {
    }
}
