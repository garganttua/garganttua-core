/**
 * Fluent builder implementation for expression contexts.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides concrete builder implementations for constructing expression
 * contexts with a fluent, type-safe API. Builders support package scanning, auto-detection,
 * and manual registration of expression nodes and leafs.
 * </p>
 *
 * <h2>Main Implementation Classes</h2>
 * <ul>
 *   <li>{@code ExpressionContextBuilder} - Builder for expression contexts</li>
 *   <li>{@code ExpressionNodeFactoryBuilder} - Builder for expression node factories</li>
 * </ul>
 *
 * <h2>Usage Example (from ExpressionContextBuilderTest)</h2>
 * <pre>{@code
 * // Build with auto-detection
 * ExpressionContext context = new ExpressionContextBuilder()
 *     .withAutoDetection()
 *     .build();
 *
 * // Build with package scanning
 * ExpressionContext context = new ExpressionContextBuilder()
 *     .withPackage("com.example.expressions")
 *     .build();
 *
 * // Build with explicit method registration
 * ExpressionContext context = new ExpressionContextBuilder()
 *     .withExpressionNodeStaticMethod(MyClass.class, "add")
 *     .build();
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Fluent, chainable API</li>
 *   <li>Package-based node discovery</li>
 *   <li>Auto-detection of expression methods</li>
 *   <li>Manual node registration</li>
 *   <li>Type-safe configuration</li>
 * </ul>
 *
 * <h2>Related Packages</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.expression.context} - Expression context implementations</li>
 *   <li>{@link com.garganttua.core.expression} - Core expression interfaces</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 */
package com.garganttua.core.expression.dsl;
