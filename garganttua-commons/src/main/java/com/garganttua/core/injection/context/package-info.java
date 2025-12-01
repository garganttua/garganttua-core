/**
 * Core dependency injection context support interfaces and utilities.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides supporting interfaces and contracts for implementing
 * dependency injection contexts. It bridges the gap between the public API
 * defined in {@link com.garganttua.core.injection} and the implementation
 * details in the garganttua-injection module.
 * </p>
 *
 * <h2>Key Concepts</h2>
 * <p>
 * The context support layer handles:
 * </p>
 * <ul>
 *   <li>Bean lifecycle management</li>
 *   <li>Property resolution strategies</li>
 *   <li>Dependency graph construction</li>
 *   <li>Scope management (singleton vs prototype)</li>
 *   <li>Context hierarchy support</li>
 * </ul>
 *
 * <h2>Related Packages</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.injection.context.dsl} - Fluent builder APIs for context configuration</li>
 *   <li>{@link com.garganttua.core.injection} - Public DI framework contracts</li>
 *   <li>{@link com.garganttua.core.injection.annotations} - DI annotations</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.injection.IDiContext
 * @see com.garganttua.core.injection.context.dsl.IDiContextBuilder
 */
package com.garganttua.core.injection.context;
