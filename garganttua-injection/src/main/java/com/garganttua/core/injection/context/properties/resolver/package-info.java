/**
 * Property value resolution strategies and resolvers.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides implementation classes for resolving property values from
 * various sources. It contains resolvers that handle type conversion, placeholder
 * substitution, default values, and source prioritization.
 * </p>
 *
 * <h2>Key Responsibilities</h2>
 * <ul>
 *   <li>Property value resolution from multiple sources</li>
 *   <li>Type conversion (String to target type)</li>
 *   <li>Placeholder expansion (${property.name})</li>
 *   <li>Default value handling</li>
 *   <li>Source priority management</li>
 *   <li>Nested placeholder resolution</li>
 * </ul>
 *
 * <h2>Resolution Strategy</h2>
 * <p>
 * Property resolution follows a priority order:
 * </p>
 * <ol>
 *   <li>Context-specific properties</li>
 *   <li>System properties</li>
 *   <li>Environment variables</li>
 *   <li>Property files</li>
 *   <li>Parent context properties</li>
 *   <li>Default values</li>
 * </ol>
 *
 * <h2>Type Conversion</h2>
 * <p>
 * Resolvers support automatic type conversion for:
 * </p>
 * <ul>
 *   <li>Primitives (int, long, boolean, etc.)</li>
 *   <li>Wrapper types (Integer, Long, Boolean, etc.)</li>
 *   <li>Strings</li>
 *   <li>Enums</li>
 *   <li>Collections (List, Set, Map)</li>
 *   <li>Arrays</li>
 *   <li>Custom types via converters</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.injection.context.properties
 * @see com.garganttua.core.injection.IPropertyProvider
 */
package com.garganttua.core.injection.context.properties.resolver;
