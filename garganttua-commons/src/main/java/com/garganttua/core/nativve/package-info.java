/**
 * Native image support and GraalVM integration utilities.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides abstractions and utilities for building GraalVM native images.
 * It helps generate reflection configuration, resource metadata, and other artifacts
 * required for ahead-of-time (AOT) compilation.
 * </p>
 *
 * <h2>Core Interfaces</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.nativve.INativeImageConfig} - Native image configuration</li>
 *   <li>{@link com.garganttua.core.nativve.IReflectionConfig} - Reflection metadata</li>
 *   <li>{@link com.garganttua.core.nativve.IResourceConfig} - Resource bundle configuration</li>
 * </ul>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Automatic reflection configuration generation</li>
 *   <li>Resource bundle registration</li>
 *   <li>JNI configuration support</li>
 *   <li>Proxy class registration</li>
 *   <li>Serialization configuration</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Register class for reflection
 * INativeImageConfig config = new NativeImageConfig();
 * config.registerForReflection(UserService.class);
 * config.registerForReflection(User.class, true, true); // all fields, all methods
 *
 * // Register resources
 * config.registerResource("application.properties");
 * config.registerResourcePattern("templates/.*\\.html");
 *
 * // Generate configuration files
 * config.writeTo(Paths.get("META-INF/native-image"));
 * }</pre>
 *
 * <h2>Integration with Maven Plugin</h2>
 * <p>
 * The {@code garganttua-native-image-maven-plugin} automates configuration generation
 * during the build process.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.nativve.INativeImageConfig
 */
package com.garganttua.core.nativve;
