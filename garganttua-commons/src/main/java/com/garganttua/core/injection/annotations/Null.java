package com.garganttua.core.injection.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.nativve.annotations.Native;

/**
 * Explicitly injects a null value into a field or parameter.
 *
 * <p>
 * The {@code @Null} annotation is used to explicitly specify that a field or parameter should
 * be injected with a null value, bypassing any default dependency resolution. This is useful
 * for optional dependencies where the absence of a value is intentional and meaningful, or
 * when overriding default behavior in specific scenarios. This annotation makes the intention
 * to use null explicit and self-documenting.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Explicit null injection in fields
 * public class OptionalFeatureService {
 *     // This feature is explicitly disabled
 *     @Null
 *     private CacheProvider cacheProvider;
 *
 *     // Optional analytics - explicitly not configured
 *     @Null
 *     private AnalyticsService analytics;
 *
 *     public void process(String data) {
 *         // Process data without caching
 *         if (cacheProvider != null) {
 *             cacheProvider.cache(data);
 *         }
 *     }
 * }
 *
 * // Constructor with explicit null parameter
 * public class ConfigurableService {
 *     private final Logger logger;
 *     private final MetricsCollector metrics;
 *
 *     public ConfigurableService(
 *         Logger logger,
 *         @Null MetricsCollector metrics) {
 *         this.logger = logger;
 *         this.metrics = metrics; // Explicitly null - metrics disabled
 *     }
 * }
 * }</pre>
 *
 * <h2>When to Use</h2>
 * <ul>
 * <li>To explicitly document that a dependency is intentionally absent</li>
 * <li>For optional features that should be disabled in certain configurations</li>
 * <li>To override default dependency resolution with null</li>
 * <li>When null is a valid and meaningful state for the dependency</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see jakarta.annotation.Nullable
 */
@Native
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Null {

}
