package com.garganttua.core.injection.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.nativve.annotations.Native;
import com.garganttua.core.reflection.annotations.Indexed;

/**
 * Indicates that a field or parameter should be injected with a configuration property value.
 *
 * <p>
 * The {@code @Property} annotation enables automatic injection of configuration values from
 * property providers into bean fields or constructor/method parameters. The property value
 * is resolved from the DI context's property providers using the specified key, with automatic
 * type conversion to the target field or parameter type. This annotation is essential for
 * externalizing configuration and enabling environment-specific settings.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Inject property into a field
 * public class DatabaseService {
 *     @Property("database.url")
 *     private String databaseUrl;
 *
 *     @Property("database.pool.size")
 *     private int poolSize;
 *
 *     @Property("database.pool.enabled")
 *     private boolean poolEnabled;
 * }
 *
 * // Inject property into constructor parameter
 * public class EmailService {
 *     private final String smtpHost;
 *     private final int smtpPort;
 *
 *     public EmailService(
 *         @Property("smtp.host") String smtpHost,
 *         @Property("smtp.port") int smtpPort) {
 *         this.smtpHost = smtpHost;
 *         this.smtpPort = smtpPort;
 *     }
 * }
 *
 * // Configure properties in the DI context
 * IInjectionContext context = InjectionContextBuilder.create()
 *     .propertyProvider("config")
 *         .withProperty(String.class, "database.url", "jdbc:mysql://localhost/mydb")
 *         .withProperty(Integer.class, "database.pool.size", 10)
 *         .withProperty(Boolean.class, "database.pool.enabled", true)
 *         .and()
 *     .build();
 * }</pre>
 *
 * <h2>Type Conversion</h2>
 * <p>
 * Property values are automatically converted to the target type. Supported conversions include:
 * </p>
 * <ul>
 * <li>Primitive types and their wrappers (int, Integer, boolean, Boolean, etc.)</li>
 * <li>String values</li>
 * <li>Numeric conversions (String to int, double, etc.)</li>
 * <li>Custom type converters if registered in the context</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.injection.IPropertyProvider
 * @see com.garganttua.core.injection.IPropertySupplier
 */
@Indexed
@Native
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Property {

    /**
     * The configuration property key.
     *
     * <p>
     * Specifies the key used to lookup the property value from the property providers.
     * The key typically follows a hierarchical naming convention (e.g., "database.url",
     * "app.server.port") for better organization.
     * </p>
     *
     * @return the property key
     */
    String value();

}
