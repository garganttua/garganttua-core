package com.garganttua.core.nativve.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark types, fields, constructors, or methods for GraalVM native image reflection.
 *
 * <p>
 * {@code @Native} is used to declaratively specify that the annotated element should be
 * registered for reflection access in GraalVM native images. This annotation is processed
 * during the build to automatically generate the reflect-config.json file.
 * </p>
 *
 * <p>
 * <strong>Usage Examples:</strong>
 * </p>
 * <pre>{@code
 * // Register all constructors and methods of a class
 * @Native(queryAllDeclaredConstructors = true, queryAllDeclaredMethods = true)
 * public class MyService {
 *     // All constructors and methods will be accessible via reflection
 * }
 *
 * // Register a specific field
 * public class MyRepository {
 *     @Native
 *     private String databaseUrl;
 * }
 *
 * // Register a specific method
 * public class MyController {
 *     @Native
 *     public void handleRequest(String requestData) {
 *         // This method will be accessible via reflection
 *     }
 * }
 *
 * // Register all fields of a class
 * @Native(allDeclaredFields = true)
 * public class ConfigurationProperties {
 *     private String host;
 *     private int port;
 *     private String username;
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see IReflectionConfigurationEntry
 * @see NativeConfigurationBuilder
 */
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Native {

    /**
     * Enables querying all declared constructors via reflection.
     *
     * @return {@code true} to register all declared constructors, {@code false} otherwise
     */
    boolean queryAllDeclaredConstructors() default false;

    /**
     * Enables querying all public constructors via reflection.
     *
     * @return {@code true} to register all public constructors, {@code false} otherwise
     */
    boolean queryAllPublicConstructors() default false;

    /**
     * Enables querying all declared methods via reflection.
     *
     * @return {@code true} to register all declared methods, {@code false} otherwise
     */
    boolean queryAllDeclaredMethods() default false;

    /**
     * Enables querying all public methods via reflection.
     *
     * @return {@code true} to register all public methods, {@code false} otherwise
     */
    boolean queryAllPublicMethods() default false;

    /**
     * Enables access to all declared inner classes via reflection.
     *
     * @return {@code true} to register all declared inner classes, {@code false} otherwise
     */
    boolean allDeclaredClasses() default false;

    /**
     * Enables access to all public inner classes via reflection.
     *
     * @return {@code true} to register all public inner classes, {@code false} otherwise
     */
    boolean allPublicClasses() default false;

    /**
     * Enables access to all declared fields via reflection.
     *
     * @return {@code true} to register all declared fields, {@code false} otherwise
     */
    boolean allDeclaredFields() default false;

}
