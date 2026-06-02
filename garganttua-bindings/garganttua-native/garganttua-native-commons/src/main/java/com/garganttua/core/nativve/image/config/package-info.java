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
 *   <li>{@link com.garganttua.core.nativve.image.config.NativeImageConfig} - resolves the
 *       standard {@code META-INF/native-image} config file locations</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Resolve the standard native-image config file locations under a base directory
 * File reflectConfig = NativeImageConfig.getReflectConfigFile(baseDir);
 * File resourceConfig = NativeImageConfig.getResourceConfigFile(baseDir);
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Reflection configuration generation</li>
 *   <li>Resource pattern configuration</li>
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
