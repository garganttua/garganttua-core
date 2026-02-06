/**
 * Expression functions for interacting with the dependency injection context.
 *
 * <p>
 * This package provides @Expression annotated functions that can be used
 * in Garganttua scripts to query beans, manage properties, and interact
 * with the injection context.
 * </p>
 *
 * <h2>Available Functions</h2>
 * <ul>
 *   <li>{@code getBean(type)} - Gets a bean by type</li>
 *   <li>{@code getBeanByRef(reference)} - Gets a bean by reference string</li>
 *   <li>{@code getBeans(type)} - Gets all beans of a type</li>
 *   <li>{@code hasBean(reference)} - Checks if a bean exists</li>
 *   <li>{@code beanProviderCount()} - Returns number of bean providers</li>
 *   <li>{@code beanCount()} - Returns total bean count</li>
 *   <li>{@code getProperty(key, type)} - Gets a property value</li>
 *   <li>{@code setProperty(provider, key, value)} - Sets a property value</li>
 *   <li>{@code hasProperty(key)} - Checks if a property exists</li>
 *   <li>{@code propertyProviderCount()} - Returns number of property providers</li>
 *   <li>{@code injectionInfo()} - Returns context summary</li>
 *   <li>{@code addBean(provider, type, bean)} - Adds a bean</li>
 *   <li>{@code addNamedBean(provider, type, name, bean)} - Adds a named bean</li>
 *   <li>{@code addSingleton(provider, type, bean)} - Adds a singleton bean</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.injection.functions.InjectionFunctions
 */
package com.garganttua.core.injection.functions;
