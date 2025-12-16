/**
 * Expression context management API for expression parsing and evaluation.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides interfaces for managing expression contexts, which are
 * responsible for parsing expression strings into evaluable expression trees and
 * maintaining registries of available expression nodes and leafs.
 * </p>
 *
 * <h2>Core Interfaces</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.expression.context.IExpressionContext} - Main context for parsing and evaluating expressions</li>
 *   <li>{@link com.garganttua.core.expression.context.IExpressionNodeContext} - Context for managing expression nodes</li>
 *   <li>{@link com.garganttua.core.expression.context.IExpressionNodeFactory} - Factory for creating expression nodes and leafs</li>
 * </ul>
 *
 * <h2>Key Responsibilities</h2>
 *
 * <h3>Expression Parsing</h3>
 * <p>
 * The expression context parses string expressions into executable expression trees
 * using registered node and leaf factories.
 * </p>
 *
 * <h3>Node Registry</h3>
 * <p>
 * Expression contexts maintain registries of available expression nodes (functions)
 * and leafs (terminals) that can be used in expressions.
 * </p>
 *
 * <h3>Type Safety</h3>
 * <p>
 * Expression contexts enforce type safety by tracking the expected return types
 * of expressions and validating node compositions.
 * </p>
 *
 * <h2>Integration</h2>
 * <p>
 * This package is implemented by the garganttua-expression module, which provides
 * ANTLR4-based parsing and concrete expression context implementations.
 * </p>
 *
 * <h2>Related Packages</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.expression} - Core expression interfaces</li>
 *   <li>{@link com.garganttua.core.expression.dsl} - Expression context builder DSL</li>
 *   <li>{@link com.garganttua.core.expression.annotations} - Expression annotations</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 */
package com.garganttua.core.expression.context;
