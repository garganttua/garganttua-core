package com.garganttua.core.expression.functions;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.reflection.IClass;

import jakarta.annotation.Nullable;

import com.garganttua.core.reflection.annotations.Reflected;
/**
 * Built-in type-conversion, string and arithmetic functions for the expression language.
 *
 * <p>
 * This class provides static methods that parse or transform input values into the
 * corresponding Java type (primitives, {@link String}, {@link IClass}). Each method is
 * annotated with {@link Expression} so it is discovered and registered by the expression
 * framework under the declared function name.
 * </p>
 *
 * <h2>Provided Functions</h2>
 * <ul>
 * <li>Primitive parsers: {@code string}, {@code int}, {@code long}, {@code double},
 *     {@code float}, {@code boolean}, {@code byte}, {@code short}, {@code char}</li>
 * <li>Class loading: {@code class} (by FQN or primitive type name)</li>
 * <li>String: {@code concatenate} (2- and 3-argument overloads)</li>
 * <li>Arithmetic: {@code increment}, {@code decrement}</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * String s = Expressions.string("hello");      // "hello"
 * int i    = Expressions.integer("42");         // 42
 * IClass<?> c = Expressions.Class("java.lang.String");
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 */
@Reflected(queryAllDeclaredMethods = true)
public class Expressions {
    private static final Logger log = Logger.getLogger(Expressions.class);

    // ========== Primitive Type Converters ==========

    /**
     * Converts any value to its String representation.
     * Handles both direct String values and any other Object types via toString().
     *
     * @param value the value to convert
     * @return the string representation of the value, or null if value is null
     */
    @Expression(name = "string", description = "Converts any value to its String representation")
    public static String string(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            return s;
        }
        return value.toString();
    }

    /**
     * Parses a string into an {@code int}.
     *
     * @param value the string representation of an integer
     * @return the parsed integer value
     * @throws ExpressionException if {@code value} cannot be parsed as an integer
     */
    @Expression(name = "int", description = "Parses a string to an Integer")
    public static int integer(@Nullable String value) {
        log.trace("Converting '{}' to Integer", value);
        try {
            int result = java.lang.Integer.parseInt(value);
            log.debug("Converted '{}' to Integer: {}", value, result);
            return result;
        } catch (NumberFormatException e) {
            throw new ExpressionException("Cannot convert '" + value + "' to Integer: " + e.getMessage());
        }
    }

    /**
     * Parses a string into a {@code long}.
     *
     * @param value the string representation of a long
     * @return the parsed long value
     * @throws ExpressionException if {@code value} cannot be parsed as a long
     */
    @Expression(name = "long", description = "Parses a string to a Long")
    public static long longnumber(@Nullable String value) {
        try {
            return java.lang.Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new ExpressionException("Cannot convert '" + value + "' to Long: " + e.getMessage());
        }
    }

    /**
     * Parses a string into a {@code double}.
     *
     * @param value the string representation of a double
     * @return the parsed double value
     * @throws ExpressionException if {@code value} cannot be parsed as a double
     */
    @Expression(name = "double", description = "Parses a string to a Double")
    public static double doublenumber(@Nullable String value) {
        try {
            return java.lang.Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new ExpressionException("Cannot convert '" + value + "' to Double: " + e.getMessage());
        }
    }

    /**
     * Parses a string into a {@code float}.
     *
     * @param value the string representation of a float
     * @return the parsed float value
     * @throws ExpressionException if {@code value} cannot be parsed as a float
     */
    @Expression(name = "float", description = "Parses a string to a Float")
    public static float floatnumber(@Nullable String value) {
        try {
            return java.lang.Float.parseFloat(value);
        } catch (NumberFormatException e) {
            throw new ExpressionException("Cannot convert '" + value + "' to Float: " + e.getMessage());
        }
    }

    /**
     * Parses a string into a {@code boolean}.
     *
     * @param value the string representation of a boolean; anything other than
     *              {@code "true"} (case-insensitive) yields {@code false}
     * @return the parsed boolean value
     */
    @Expression(name = "boolean", description = "Parses a string to a Boolean (true/false)")
    public static boolean booleanValue(@Nullable String value) {
        return java.lang.Boolean.parseBoolean(value);
    }

    /**
     * Parses a string into a {@code byte}.
     *
     * @param value the string representation of a byte
     * @return the parsed byte value
     * @throws ExpressionException if {@code value} cannot be parsed as a byte
     */
    @Expression(name = "byte", description = "Parses a string to a Byte (-128 to 127)")
    public static byte byteValue(@Nullable String value) {
        try {
            return java.lang.Byte.parseByte(value);
        } catch (NumberFormatException e) {
            throw new ExpressionException("Cannot convert '" + value + "' to Byte: " + e.getMessage());
        }
    }

    /**
     * Parses a string into a {@code short}.
     *
     * @param value the string representation of a short
     * @return the parsed short value
     * @throws ExpressionException if {@code value} cannot be parsed as a short
     */
    @Expression(name = "short", description = "Parses a string to a Short")
    public static short shortNumber(@Nullable String value) {
        try {
            return java.lang.Short.parseShort(value);
        } catch (NumberFormatException e) {
            throw new ExpressionException("Cannot convert '" + value + "' to Short: " + e.getMessage());
        }
    }

    /**
     * Extracts the first character of a string as a {@code char}.
     *
     * @param value the source string (only the first character is used)
     * @return the first character of {@code value}
     * @throws ExpressionException if {@code value} is {@code null} or empty
     */
    @Expression(name = "char", description = "Extracts first character from string as Character")
    public static char character(@Nullable String value) {
        if (value == null || value.isEmpty()) {
            throw new ExpressionException("Cannot convert empty string to Character");
        }
        return value.charAt(0);
    }

    // ========== Class Type Converter ==========

    /**
     * Resolves a class by name into an {@link IClass}.
     *
     * <p>Accepts primitive type names ({@code int}, {@code boolean}, {@code void}, ...) and
     * fully qualified class names; a simple unqualified name is also retried under the
     * {@code java.lang.} package.</p>
     *
     * @param className the fully qualified class name or primitive type name
     * @return the {@link IClass} mirror for the resolved type
     * @throws ExpressionException if {@code className} is {@code null} or cannot be resolved
     */
    @Expression(name = "class", description = "Loads a class by fully qualified name or primitive type")
    public static IClass<?> Class(@Nullable String className) {
        log.trace("Loading class: {}", className);
        if (className == null) {
            throw new ExpressionException("Class name cannot be null");
        }

        // Handle primitive types
        switch (className) {
            case "boolean":
                log.debug("Returning primitive type: boolean.class");
                return IClass.getClass(boolean.class);
            case "byte":
                log.debug("Returning primitive type: byte.class");
                return IClass.getClass(byte.class);
            case "short":
                log.debug("Returning primitive type: short.class");
                return IClass.getClass(short.class);
            case "int":
                log.debug("Returning primitive type: int.class");
                return IClass.getClass(int.class);
            case "long":
                log.debug("Returning primitive type: long.class");
                return IClass.getClass(long.class);
            case "float":
                log.debug("Returning primitive type: float.class");
                return IClass.getClass(float.class);
            case "double":
                log.debug("Returning primitive type: double.class");
                return IClass.getClass(double.class);
            case "char":
                log.debug("Returning primitive type: char.class");
                return IClass.getClass(char.class);
            case "void":
                log.debug("Returning primitive type: void.class");
                return IClass.getClass(void.class);
        }

        // Handle regular classes
        try {
            IClass<?> clazz = IClass.forName(className);
            log.debug("Loaded class: {}", className);
            return clazz;
        } catch (ClassNotFoundException e) {
            // Try with java.lang. prefix for common classes (e.g., "String" -> "java.lang.String")
            if (!className.contains(".")) {
                try {
                    IClass<?> clazz = IClass.forName("java.lang." + className);
                    log.debug("Loaded class with java.lang prefix: {}", className);
                    return clazz;
                } catch (ClassNotFoundException e2) {
                    // Fall through to original error
                }
            }
            throw new ExpressionException("Cannot load class '" + className + "': " + e.getMessage());
        }
    }

    // ========== String Functions ==========

    /**
     * Concatenates two values into a single string.
     *
     * @param value1 the first value
     * @param value2 the second value
     * @return the concatenation of both values as strings
     */
    @Expression(name = "concatenate", description = "Concatenates two values into a string")
    public static String concatenate(@Nullable Object value1, @Nullable Object value2) {
        String s1 = value1 == null ? "" : value1.toString();
        String s2 = value2 == null ? "" : value2.toString();
        return s1 + s2;
    }

    /**
     * Concatenates three values into a single string.
     *
     * @param value1 the first value
     * @param value2 the second value
     * @param value3 the third value
     * @return the concatenation of all values as strings
     */
    @Expression(name = "concatenate", description = "Concatenates three values into a string")
    public static String concatenate(@Nullable Object value1, @Nullable Object value2, @Nullable Object value3) {
        String s1 = value1 == null ? "" : value1.toString();
        String s2 = value2 == null ? "" : value2.toString();
        String s3 = value3 == null ? "" : value3.toString();
        return s1 + s2 + s3;
    }

    // ========== Arithmetic Functions ==========

    /**
     * Increments a numeric or numeric-string value by one.
     *
     * @param value a {@link Number} or a string parseable as an integer
     * @return the value plus one, as an {@code int}
     * @throws ExpressionException if {@code value} is null, non-numeric, or an unsupported type
     */
    @Expression(name = "increment", description = "Increments an integer value by 1")
    public static int increment(@Nullable Object value) {
        if (value instanceof Number n) {
            return n.intValue() + 1;
        }
        if (value instanceof String s) {
            try {
                return java.lang.Integer.parseInt(s) + 1;
            } catch (NumberFormatException e) {
                throw new ExpressionException("Cannot increment non-numeric value: '" + s + "'");
            }
        }
        throw new ExpressionException("Cannot increment value of type: " + (value == null ? "null" : value.getClass().getName()));
    }

    /**
     * Decrements a numeric or numeric-string value by one.
     *
     * @param value a {@link Number} or a string parseable as an integer
     * @return the value minus one, as an {@code int}
     * @throws ExpressionException if {@code value} is null, non-numeric, or an unsupported type
     */
    @Expression(name = "decrement", description = "Decrements an integer value by 1")
    public static int decrement(@Nullable Object value) {
        if (value instanceof Number n) {
            return n.intValue() - 1;
        }
        if (value instanceof String s) {
            try {
                return java.lang.Integer.parseInt(s) - 1;
            } catch (NumberFormatException e) {
                throw new ExpressionException("Cannot decrement non-numeric value: '" + s + "'");
            }
        }
        throw new ExpressionException("Cannot decrement value of type: " + (value == null ? "null" : value.getClass().getName()));
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private Expressions() {
    }
}
