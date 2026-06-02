/**
 * GraalVM Native Image reflection configuration generation.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides classes for generating GraalVM reflection configuration
 * (reflect-config.json). It enables programmatic creation of reflection metadata
 * required for native compilation.
 * </p>
 *
 * <h2>Main Classes</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.nativve.image.config.reflection.ReflectionConfiguration} -
 *       in-memory model of a {@code reflect-config.json} file (load/save)</li>
 *   <li>{@link com.garganttua.core.nativve.image.config.reflection.ReflectConfigEntry} -
 *       single reflection entry</li>
 *   <li>{@link com.garganttua.core.nativve.image.config.reflection.ReflectConfigEntryBuilder} -
 *       fluent builder for reflection entries</li>
 * </ul>
 *
 * <h2>Usage Example: Builder API</h2>
 * <pre>{@code
 * // Use the builder for fine-grained control over a single entry
 * IReflectionConfigurationEntry entry = ReflectConfigEntryBuilder.builder(orderServiceClass)
 *     .queryAllDeclaredConstructors(true)
 *     .allDeclaredFields(false)
 *     .method("processOrder", orderClass)
 *     .field("orderRepository")
 *     .build();
 *
 * ReflectionConfiguration config = ReflectionConfiguration.loadFromFile(reflectConfigFile);
 * config.addEntry(entry);
 * config.saveToFile(reflectConfigFile);
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Class-level reflection configuration</li>
 *   <li>Method registration (by name and signature)</li>
 *   <li>Field registration</li>
 *   <li>Constructor registration</li>
 *   <li>Bulk "all/query-all declared members" flags</li>
 *   <li>Automatic detection of {@code @Reflected} members</li>
 *   <li>Fluent builder API</li>
 *   <li>JSON serialization</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.nativve.image.config
 */
package com.garganttua.core.nativve.image.config.reflection;
