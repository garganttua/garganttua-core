package com.garganttua.core.injection.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.nativve.annotations.Native;

/**
 * Injects a fixed literal value into a field or parameter.
 *
 * <p>
 * The {@code @Fixed} annotation allows injection of compile-time constant values directly into
 * fields or constructor/method parameters without requiring property configuration or bean
 * definitions. This is useful for default values, magic numbers with clear semantics, or
 * simple constant injection scenarios. Unlike {@code @Property}, these values are hard-coded
 * in the annotation and cannot be externalized.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Inject fixed values into fields
 * public class CacheService {
 *     @Fixed(valueInt = 1000)
 *     private int maxEntries;
 *
 *     @Fixed(valueString = "default-cache")
 *     private String cacheName;
 *
 *     @Fixed(valueBoolean = true)
 *     private boolean enableAutoEviction;
 *
 *     @Fixed(valueDouble = 0.75)
 *     private double loadFactor;
 * }
 *
 * // Constructor injection with fixed values
 * public class ConnectionPool {
 *     private final int minConnections;
 *     private final int maxConnections;
 *     private final long timeoutMillis;
 *
 *     public ConnectionPool(
 *         @Fixed(valueInt = 5) int minConnections,
 *         @Fixed(valueInt = 20) int maxConnections,
 *         @Fixed(valueLong = 30000L) long timeoutMillis) {
 *         this.minConnections = minConnections;
 *         this.maxConnections = maxConnections;
 *         this.timeoutMillis = timeoutMillis;
 *     }
 * }
 * }</pre>
 *
 * <h2>Type Selection</h2>
 * <p>
 * Only one value* element should be set per annotation. The DI framework uses the target
 * parameter/field type to determine which value to inject. If multiple values are set,
 * the framework selects based on type compatibility.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 * @see Property
 */
@Native
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Fixed {

    /**
     * Fixed integer value.
     * @return the integer value to inject
     */
    int valueInt() default -1;

    /**
     * Fixed double value.
     * @return the double value to inject
     */
    double valueDouble() default -1;

    /**
     * Fixed float value.
     * @return the float value to inject
     */
    float valueFloat() default -1;

    /**
     * Fixed long value.
     * @return the long value to inject
     */
    long valueLong() default -1;

    /**
     * Fixed string value.
     * @return the string value to inject
     */
    String valueString() default "default";

    /**
     * Fixed byte value.
     * @return the byte value to inject
     */
    byte valueByte() default -1;

    /**
     * Fixed short value.
     * @return the short value to inject
     */
    short valueShort()default -1;

    /**
     * Fixed boolean value.
     * @return the boolean value to inject
     */
    boolean valueBoolean() default false;

    /**
     * Fixed character value.
     * @return the character value to inject
     */
    char valueChar() default '\u0000';

}
