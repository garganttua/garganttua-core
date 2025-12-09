/**
 * DSL (Domain Specific Language) implementation for building Expression instances.
 *
 * <p>
 * This package provides a fluent API for constructing {@link com.garganttua.core.expression.IExpression}
 * objects through builder patterns. The DSL simplifies the creation of complex expressions by providing
 * a type-safe, chainable interface.
 * </p>
 *
 * <h2>Main Components</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.expression.dsl.ExpressionBuilder} - Main entry point for building expressions</li>
 *   <li>{@link com.garganttua.core.expression.dsl.ExpressionMethodBinderBuilder} - Builder for binding methods to expressions</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create a simple expression that calls a method
 * IExpression<String, ISupplier<String>> expression = ExpressionBuilder
 *     .create(String.class)
 *     .withExpression(StringUtils.class, String.class)
 *         .method("concat")
 *         .withParam("Hello")
 *         .withParam(" World")
 *         .end()
 *     .build();
 *
 * // Evaluate the expression
 * ISupplier<String> result = expression.evaluate();
 * String value = result.get(); // "Hello World"
 *
 * // Create an expression with auto-detection
 * IExpression<Integer, ISupplier<Integer>> autoExpr = ExpressionBuilder
 *     .create(Integer.class)
 *     .autoDetect(true)
 *     .withPackage("com.example.expressions")
 *     .build();
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Fluent API for expression construction</li>
 *   <li>Type-safe method binding</li>
 *   <li>Support for both simple and contextual expressions</li>
 *   <li>Auto-detection of expressions from packages</li>
 *   <li>Integration with the Garganttua reflection and supply systems</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.expression.IExpression
 * @see com.garganttua.core.expression.IExpressionMethodBinder
 * @see com.garganttua.core.supply.ISupplier
 */
package com.garganttua.core.expression.dsl;
