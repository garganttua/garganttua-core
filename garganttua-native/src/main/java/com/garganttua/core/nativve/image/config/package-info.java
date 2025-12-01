/**
 * Native image configuration file generation and management.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides classes for generating GraalVM Native Image configuration files.
 * It supports generating reflection, resource, and other configuration files required
 * for successful native compilation.
 * </p>
 *
 * <h2>Main Classes</h2>
 * <ul>
 *   <li>{@code NativeImageConfig} - Main native image configuration</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create native image configuration
 * NativeImageConfig config = new NativeImageConfig();
 *
 * // Add reflection configuration
 * config.addReflectionClass(UserService.class);
 * config.addReflectionClass(DataRepository.class);
 *
 * // Add resource patterns
 * config.addResourcePattern("*.properties");
 * config.addResourcePattern("templates/*.html");
 *
 * // Generate configuration files
 * config.writeToDirectory(outputDir);
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Reflection configuration generation</li>
 *   <li>Resource pattern configuration</li>
 *   <li>JNI configuration support</li>
 *   <li>Serialization configuration</li>
 *   <li>JSON format output</li>
 * </ul>
 *
 * <h2>Related Packages</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.nativve.image.config.reflection} - Reflection configuration</li>
 *   <li>{@link com.garganttua.core.nativve.image.config.resources} - Resource configuration</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 */
package com.garganttua.core.nativve.image.config;
