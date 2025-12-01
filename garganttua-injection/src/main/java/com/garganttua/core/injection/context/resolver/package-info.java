/**
 * Injectable element detection and resolution implementation.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides implementation for detecting and resolving injectable elements
 * (fields, constructors, methods) in bean classes. It scans classes for injection
 * annotations and builds resolution strategies.
 * </p>
 *
 * <h2>Key Responsibilities</h2>
 * <ul>
 *   <li>Scan classes for @Inject annotations</li>
 *   <li>Identify injectable constructors</li>
 *   <li>Identify injectable fields</li>
 *   <li>Identify injectable setter methods</li>
 *   <li>Detect @Property annotations</li>
 *   <li>Detect @Provider annotations</li>
 *   <li>Detect qualifier annotations</li>
 *   <li>Build injection metadata</li>
 * </ul>
 *
 * <h2>Detection Strategy</h2>
 * <ol>
 *   <li>Scan class for @Inject constructor (or default constructor)</li>
 *   <li>Scan fields for @Inject annotation</li>
 *   <li>Scan methods for @Inject annotation (setter methods)</li>
 *   <li>Scan for lifecycle annotations (@PostConstruct, @PreDestroy)</li>
 *   <li>Build injection plan</li>
 * </ol>
 *
 * <h2>Resolution Metadata</h2>
 * <p>
 * Resolvers build metadata including:
 * </p>
 * <ul>
 *   <li>Injection points (fields, constructors, methods)</li>
 *   <li>Dependency types</li>
 *   <li>Qualifiers</li>
 *   <li>Property placeholders</li>
 *   <li>Optional dependencies</li>
 *   <li>Default values</li>
 *   <li>Lifecycle callbacks</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.injection.context
 * @see com.garganttua.core.injection.IInjectableElementResolverBuilder
 */
package com.garganttua.core.injection.context.resolver;
