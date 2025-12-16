/**
 * Expression evaluation framework API for dynamic expression parsing and evaluation.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides the core interfaces for the expression evaluation framework.
 * It defines contracts for expressions, expression nodes, and evaluation strategies
 * that enable parsing and executing domain-specific expression languages at runtime.
 * </p>
 *
 * <h2>Core Interfaces</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.expression.IExpression} - Base expression interface that extends supplier builder</li>
 *   <li>{@link com.garganttua.core.expression.IExpressionNode} - Expression node that can be evaluated to produce a supplier</li>
 *   <li>{@link com.garganttua.core.expression.IEvaluateNode} - Node evaluation interface for complex expressions</li>
 *   <li>{@link com.garganttua.core.expression.IEvaluateLeaf} - Leaf evaluation interface for terminal values</li>
 *   <li>{@link com.garganttua.core.expression.IContextualExpressionNode} - Contextual expression node with context awareness</li>
 *   <li>{@link com.garganttua.core.expression.IContextualEvaluate} - Contextual evaluation with external context</li>
 * </ul>
 *
 * <h2>Exception Handling</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.expression.ExpressionException} - Exception thrown during expression evaluation</li>
 * </ul>
 *
 * <h2>Key Concepts</h2>
 *
 * <h3>Expression Nodes</h3>
 * <p>
 * Expression nodes represent intermediate or terminal elements in an expression tree.
 * They can be evaluated to produce suppliers of values, enabling lazy evaluation.
 * </p>
 *
 * <h3>Contextual Evaluation</h3>
 * <p>
 * Contextual expressions can access external context during evaluation, enabling
 * dependency injection, variable resolution, and dynamic behavior based on runtime state.
 * </p>
 *
 * <h3>Supplier Integration</h3>
 * <p>
 * Expressions integrate with the Garganttua supply framework, producing {@code ISupplier}
 * instances that can be evaluated lazily and composed with other framework components.
 * </p>
 *
 * <h2>Integration</h2>
 * <p>
 * This package is implemented by the garganttua-expression module, which provides
 * concrete expression parsing, ANTLR4 integration, and expression context management.
 * </p>
 *
 * <h2>Related Packages</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.expression.annotations} - Expression annotations (@Expression, @ExpressionNode, @ExpressionLeaf)</li>
 *   <li>{@link com.garganttua.core.expression.context} - Expression context management</li>
 *   <li>{@link com.garganttua.core.expression.dsl} - Expression builder DSL</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.supply
 */
package com.garganttua.core.expression;
