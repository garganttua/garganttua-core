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
 *   <li>{@code ReflectConfig} - Main reflection configuration</li>
 *   <li>{@code ReflectConfigEntry} - Single reflection entry</li>
 *   <li>{@code ReflectConfigEntryBuilder} - Builder for reflection entries</li>
 *   <li>{@code IReflectConfigEntryBuilder} - Builder interface</li>
 * </ul>
 *
 * <h2>Usage Example: Simple Configuration</h2>
 * <pre>{@code
 * // Create reflection configuration
 * ReflectConfig config = new ReflectConfig();
 *
 * // Add class with all methods and fields
 * config.addClass(UserService.class)
 *     .allDeclaredMethods(true)
 *     .allDeclaredFields(true);
 *
 * // Add class with specific methods
 * config.addClass(DataRepository.class)
 *     .method("findById", String.class)
 *     .method("save", Object.class);
 *
 * // Write to file
 * config.writeToFile("reflect-config.json");
 * }</pre>
 *
 * <h2>Usage Example: Builder API</h2>
 * <pre>{@code
 * // Use builder for fine-grained control
 * ReflectConfigEntry entry = new ReflectConfigEntryBuilder()
 *     .className(OrderService.class.getName())
 *     .allDeclaredConstructors(true)
 *     .allDeclaredMethods(false)
 *     .allDeclaredFields(false)
 *     .addMethod("processOrder", Order.class)
 *     .addMethod("validateOrder", Order.class)
 *     .addField("orderRepository")
 *     .build();
 *
 * ReflectConfig config = new ReflectConfig();
 * config.addEntry(entry);
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Class-level reflection configuration</li>
 *   <li>Method registration (by name and signature)</li>
 *   <li>Field registration</li>
 *   <li>Constructor registration</li>
 *   <li>All declared members flags</li>
 *   <li>Fluent builder API</li>
 *   <li>JSON serialization</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.nativve.image.config
 */
package com.garganttua.core.nativve.image.config.reflection;
