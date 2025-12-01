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
 *   <li>{@code ResourceConfig} - Main resource configuration</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create resource configuration
 * ResourceConfig config = new ResourceConfig();
 *
 * // Add resource patterns
 * config.addPattern("application.properties");
 * config.addPattern("templates/all.html");
 * config.addPattern("static/all.css");
 * config.addPattern("static/all.js");
 *
 * // Add resource bundles
 * config.addBundle("messages");
 * config.addBundle("ValidationMessages");
 *
 * // Write to file
 * config.writeToFile("resource-config.json");
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Resource pattern registration</li>
 *   <li>Resource bundle registration</li>
 *   <li>Glob pattern support</li>
 *   <li>JSON serialization</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.nativve.image.config
 */
package com.garganttua.core.nativve.image.config.resources;
