/**
 * Bean dependency resolution strategies and resolvers.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides implementation classes for resolving bean dependencies during
 * injection. It contains resolvers that handle constructor parameter resolution, field
 * value resolution, and complex dependency graph traversal.
 * </p>
 *
 * <h2>Key Responsibilities</h2>
 * <ul>
 *   <li>Constructor parameter dependency resolution</li>
 *   <li>Field injection value resolution</li>
 *   <li>Setter method parameter resolution</li>
 *   <li>Circular dependency detection</li>
 *   <li>Qualifier-based bean matching</li>
 *   <li>Type-based bean matching</li>
 *   <li>Property placeholder resolution for injected values</li>
 * </ul>
 *
 * <h2>Resolution Strategies</h2>
 * <ul>
 *   <li><b>By Type</b> - Match bean by class type</li>
 *   <li><b>By Qualifier</b> - Match bean by qualifier annotation</li>
 *   <li><b>By Name</b> - Match bean by name</li>
 *   <li><b>By Property</b> - Resolve property value</li>
 *   <li><b>By Provider</b> - Use provider method</li>
 *   <li><b>By Supplier</b> - Use supplier function</li>
 * </ul>
 *
 * <h2>Dependency Graph</h2>
 * <p>
 * Resolvers build and traverse the dependency graph to:
 * </p>
 * <ul>
 *   <li>Determine injection order</li>
 *   <li>Detect circular dependencies</li>
 *   <li>Resolve transitive dependencies</li>
 *   <li>Optimize bean creation</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.injection.context.beans
 * @see com.garganttua.core.injection.IBeanFactory
 */
package com.garganttua.core.injection.context.beans.resolver;
