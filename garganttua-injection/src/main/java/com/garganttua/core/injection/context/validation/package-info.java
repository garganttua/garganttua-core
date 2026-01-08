/**
 * DI context and bean configuration validation.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides validation logic for DI context configuration, bean definitions,
 * and dependency graphs. It ensures that the context is properly configured before
 * startup and that all dependencies can be satisfied.
 * </p>
 *
 * <h2>Validation Types</h2>
 * <ul>
 *   <li><b>Configuration Validation</b> - Validates context builder configuration</li>
 *   <li><b>Bean Definition Validation</b> - Validates bean class definitions</li>
 *   <li><b>Dependency Validation</b> - Validates dependency relationships</li>
 *   <li><b>Scope Validation</b> - Validates scope compatibility</li>
 *   <li><b>Lifecycle Validation</b> - Validates lifecycle callback methods</li>
 * </ul>
 *
 * <h2>Validation Checks</h2>
 * <ul>
 *   <li>All dependencies can be resolved</li>
 *   <li>No circular dependencies exist</li>
 *   <li>Bean classes are instantiable</li>
 *   <li>Required properties are present</li>
 *   <li>Qualifiers are properly configured</li>
 *   <li>Scopes are compatible</li>
 *   <li>Post-construct methods are valid</li>
 *   <li>Pre-destroy methods are valid</li>
 *   <li>Provider methods return correct types</li>
 * </ul>
 *
 * <h2>Validation Timing</h2>
 * <ul>
 *   <li><b>Build Time</b> - Context configuration validation</li>
 *   <li><b>Startup Time</b> - Dependency graph validation</li>
 *   <li><b>Runtime</b> - Bean instantiation validation</li>
 * </ul>
 *
 * <h2>Error Reporting</h2>
 * <p>
 * Validators provide detailed error messages including:
 * </p>
 * <ul>
 *   <li>Bean class name</li>
 *   <li>Missing dependency details</li>
 *   <li>Circular dependency chain</li>
 *   <li>Configuration issues</li>
 *   <li>Suggestions for fixes</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.injection.context
 * @see com.garganttua.core.injection.IInjectionContext
 */
package com.garganttua.core.injection.context.validation;
