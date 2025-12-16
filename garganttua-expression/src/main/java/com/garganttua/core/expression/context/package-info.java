/**
 * Expression context management implementation with ANTLR4 integration.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides the concrete implementation of expression context management.
 * It implements ANTLR4-based expression parsing, node registry management, and
 * expression evaluation coordination.
 * </p>
 *
 * <h2>Main Implementation Classes</h2>
 * <ul>
 *   <li>{@code ExpressionContext} - Main expression context implementation</li>
 *   <li>{@code ExpressionNodeContext} - Expression node context implementation</li>
 *   <li>{@code ExpressionNodeFactory} - Factory for creating expression nodes</li>
 * </ul>
 *
 * <h2>Usage Example (from ExpressionContextTest)</h2>
 * <pre>{@code
 * // Create expression context
 * ExpressionContext context = new ExpressionContextBuilder()
 *     .withPackage("com.example.expressions")
 *     .withAutoDetection()
 *     .build();
 *
 * // Parse and evaluate expression
 * ISupplier<String> result = context.parse("concat(\"Hello\", \"World\")", String.class);
 * assertEquals("HelloWorld", result.supply().get());
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>ANTLR4-based expression parsing</li>
 *   <li>Automatic expression node discovery</li>
 *   <li>Type-safe expression evaluation</li>
 *   <li>Method binding for expression functions</li>
 *   <li>Package-based node registration</li>
 * </ul>
 *
 * <h2>Related Packages</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.expression} - Core expression interfaces</li>
 *   <li>{@link com.garganttua.core.expression.dsl} - Expression builder DSL</li>
 *   <li>{@link com.garganttua.core.expression.functions} - Standard expression functions</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 */
package com.garganttua.core.expression.context;
