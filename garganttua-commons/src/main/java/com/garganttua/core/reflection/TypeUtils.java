package com.garganttua.core.reflection;

import java.util.Map;
import java.util.Set;

/**
 * Utility class for primitive/wrapper type mapping, assignability checks,
 * and complex type detection.
 *
 * <p>
 * Centralises logic that was previously duplicated across
 * {@code Constructors}, {@code ConstructorDelegate}, {@code TypeDelegate},
 * and {@code Fields}.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 */
public final class TypeUtils {

    private TypeUtils() {
        /* Utility class — not instantiable */
    }

    // --- Primitive → Wrapper mapping (Class-based, for runtime use) ---

    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER = Map.of(
            boolean.class, Boolean.class,
            byte.class, Byte.class,
            char.class, Character.class,
            short.class, Short.class,
            int.class, Integer.class,
            long.class, Long.class,
            float.class, Float.class,
            double.class, Double.class,
            void.class, Void.class);

    // --- Primitive name → Wrapper FQN mapping (String-based, for IClass use) ---

    private static final Map<String, String> PRIMITIVE_NAME_TO_WRAPPER_NAME = Map.of(
            "boolean", "java.lang.Boolean",
            "byte", "java.lang.Byte",
            "char", "java.lang.Character",
            "short", "java.lang.Short",
            "int", "java.lang.Integer",
            "long", "java.lang.Long",
            "float", "java.lang.Float",
            "double", "java.lang.Double",
            "void", "java.lang.Void");

    private static final Set<String> WRAPPER_TYPE_NAMES = Set.of(
            "java.lang.Boolean",
            "java.lang.Byte",
            "java.lang.Character",
            "java.lang.Short",
            "java.lang.Integer",
            "java.lang.Long",
            "java.lang.Float",
            "java.lang.Double",
            "java.lang.Void");

    private static final Set<String> SIMPLE_TYPE_NAMES = Set.of(
            "java.lang.Boolean",
            "java.lang.Byte",
            "java.lang.Character",
            "java.lang.Short",
            "java.lang.Integer",
            "java.lang.Long",
            "java.lang.Float",
            "java.lang.Double",
            "java.lang.String",
            "java.util.Date");

    // --- Class-based API ---

    /**
     * Returns the wrapper class for a primitive class, or {@code null} if the
     * given class is not primitive.
     */
    public static Class<?> wrapperFor(Class<?> primitiveClass) {
        return PRIMITIVE_TO_WRAPPER.get(primitiveClass);
    }

    // --- String/IClass-based API ---

    /**
     * Returns the wrapper FQN for a primitive name (e.g. "int" → "java.lang.Integer").
     */
    public static String wrapperNameFor(String primitiveName) {
        return PRIMITIVE_NAME_TO_WRAPPER_NAME.get(primitiveName);
    }

    /**
     * Returns {@code true} if the given FQN is a wrapper type (e.g. "java.lang.Integer").
     */
    public static boolean isWrapperType(String className) {
        return WRAPPER_TYPE_NAMES.contains(className);
    }

    /**
     * Checks assignability between two {@link IClass} instances, including
     * primitive/wrapper auto-boxing equivalence.
     *
     * @param formal the declared (target) type
     * @param actual the value (source) type
     * @return {@code true} if {@code actual} is assignable to {@code formal}
     */
    public static boolean isAssignable(IClass<?> formal, IClass<?> actual) {
        if (formal.isAssignableFrom(actual)) {
            return true;
        }
        if (formal.isPrimitive()) {
            String wrapName = PRIMITIVE_NAME_TO_WRAPPER_NAME.get(formal.getName());
            if (wrapName != null && actual.getName().equals(wrapName)) {
                return true;
            }
        }
        if (actual.isPrimitive()) {
            String wrapName = PRIMITIVE_NAME_TO_WRAPPER_NAME.get(actual.getName());
            if (wrapName != null && formal.getName().equals(wrapName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the class is a "complex" (user-defined / domain) type,
     * i.e. not a primitive, wrapper, String, Date, or JDK-internal type.
     */
    public static boolean isComplexType(IClass<?> clazz) {
        if (clazz.isPrimitive()) {
            return false;
        }
        if (SIMPLE_TYPE_NAMES.contains(clazz.getName())) {
            return false;
        }
        Package p = clazz.getPackage();
        if (p == null) {
            return false;
        }
        String packageName = p.getName();
        return !packageName.startsWith("java.")
                && !packageName.startsWith("javax.")
                && !packageName.startsWith("sun.")
                && !packageName.startsWith("jdk.");
    }

    /**
     * Returns {@code true} if the class is NOT a primitive or primitive-like type
     * (i.e. not a primitive, wrapper, String, or Date).
     * <p>
     * This is the inverse of the "simple type" concept: primitives, their wrappers,
     * String, and Date are considered "not complex primitives".
     * </p>
     */
    public static boolean isNotPrimitive(IClass<?> clazz) {
        if (clazz.isPrimitive()) {
            return false;
        }
        return !SIMPLE_TYPE_NAMES.contains(clazz.getName());
    }

    /**
     * Returns {@code true} if the class is not a primitive/simple type AND not
     * a JDK internal type. Equivalent to {@link #isComplexType(IClass)}.
     */
    public static boolean isNotPrimitiveOrInternal(IClass<?> clazz) {
        return isComplexType(clazz);
    }
}
