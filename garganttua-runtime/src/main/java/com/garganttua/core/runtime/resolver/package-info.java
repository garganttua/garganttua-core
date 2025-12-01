/**
 * Runtime parameter and dependency resolution strategies.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides implementation classes for resolving runtime execution
 * parameters. It handles annotation-based parameter resolution, type matching,
 * and value extraction from the runtime context.
 * </p>
 *
 * <h2>Resolution Strategies</h2>
 * <ul>
 *   <li><b>Input Resolution</b> - Resolve {@code @Input} annotated parameters</li>
 *   <li><b>Output Resolution</b> - Resolve {@code @Output} annotated parameters</li>
 *   <li><b>Context Resolution</b> - Resolve {@code @Context} annotated parameters</li>
 *   <li><b>Variable Resolution</b> - Resolve {@code @Variable} annotated parameters</li>
 *   <li><b>Exception Resolution</b> - Resolve {@code @Exception} annotated parameters</li>
 *   <li><b>Code Resolution</b> - Resolve {@code @Code} annotated parameters</li>
 * </ul>
 *
 * <h2>Parameter Detection</h2>
 * <p>
 * Resolvers scan step method parameters for annotations:
 * </p>
 * <ol>
 *   <li>Detect parameter annotations</li>
 *   <li>Determine resolution strategy</li>
 *   <li>Extract value from runtime context</li>
 *   <li>Perform type conversion if needed</li>
 *   <li>Inject resolved value</li>
 * </ol>
 *
 * <h2>Type Matching</h2>
 * <p>
 * Resolvers ensure type compatibility:
 * </p>
 * <ul>
 *   <li>Input type matches runtime input type</li>
 *   <li>Output type matches runtime output type</li>
 *   <li>Variable type matches declared type</li>
 *   <li>Exception type is assignable from thrown exception</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.runtime
 * @see com.garganttua.core.runtime.annotations
 */
package com.garganttua.core.runtime.resolver;
