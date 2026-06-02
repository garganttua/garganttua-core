/**
 * GraalVM Native Image resource configuration generation.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides classes for generating GraalVM resource configuration
 * (resource-config.json). It enables programmatic specification of resources
 * that should be included in the native image.
 * </p>
 *
 * <h2>Main Classes</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.nativve.image.config.resources.ResourceConfig} - static
 *       utility for reading/writing {@code resource-config.json} include patterns</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Add and remove resource include patterns directly in the config file
 * ResourceConfig.addResource(resourceConfigFile, "application.properties");
 * ResourceConfig.addResource(resourceConfigFile, userServiceClass);
 * ResourceConfig.removeResource(resourceConfigFile, "application.properties");
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Resource pattern registration (literal-quoted)</li>
 *   <li>Class-to-resource path conversion</li>
 *   <li>Duplicate-safe add and idempotent remove</li>
 *   <li>JSON serialization</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.nativve.image.config
 */
package com.garganttua.core.nativve.image.config.resources;
