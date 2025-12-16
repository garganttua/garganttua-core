/**
 * Fluent builder API for constructing expression contexts.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides builder interfaces for constructing expression contexts
 * using a fluent, type-safe DSL. Builders enable configuration of expression
 * contexts with custom nodes, leafs, and parsing strategies.
 * </p>
 *
 * <h2>Core Interfaces</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.expression.dsl.IExpressionContextBuilder} - Builder for expression contexts</li>
 *   <li>{@link com.garganttua.core.expression.dsl.IExpressionMethodBinderBuilder} - Builder for method binding in expressions</li>
 * </ul>
 *
 * <h2>Builder Pattern</h2>
 * <p>
 * Expression context builders follow the fluent builder pattern, allowing
 * method chaining for readable configuration:
 * </p>
 * <pre>{@code
 * IExpressionContext context = builder
 *     .withPackage("com.example.expressions")
 *     .withAutoDetection()
 *     .build();
 * }</pre>
 *
 * <h2>Related Packages</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.expression.context} - Expression context interfaces</li>
 *   <li>{@link com.garganttua.core.expression} - Core expression interfaces</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 */
package com.garganttua.core.expression.dsl;
