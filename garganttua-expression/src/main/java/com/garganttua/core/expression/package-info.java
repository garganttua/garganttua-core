/**
 * Expression evaluation framework implementation.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides the concrete implementation of the expression evaluation framework.
 * It implements expression nodes, leafs, and contextual expressions for parsing and evaluating
 * domain-specific expression languages at runtime.
 * </p>
 *
 * <h2>Main Implementation Classes</h2>
 * <ul>
 *   <li>{@code Expression} - Base expression implementation</li>
 *   <li>{@code ExpressionNode} - Expression node for complex expressions</li>
 *   <li>{@code ExpressionLeaf} - Expression leaf for terminal values</li>
 *   <li>{@code ContextualExpressionNode} - Contextual expression node with context awareness</li>
 * </ul>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Type-safe expression evaluation</li>
 *   <li>Lazy evaluation through suppliers</li>
 *   <li>Contextual expression support</li>
 *   <li>Integration with supply framework</li>
 *   <li>Exception handling</li>
 * </ul>
 *
 * <h2>Related Packages</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.expression.context} - Expression context management</li>
 *   <li>{@link com.garganttua.core.expression.dsl} - Expression builder DSL</li>
 *   <li>{@link com.garganttua.core.expression.functions} - Standard expression functions</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.expression
 */
package com.garganttua.core.expression;
