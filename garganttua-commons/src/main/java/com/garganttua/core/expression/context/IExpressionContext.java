package com.garganttua.core.expression.context;

import com.garganttua.core.expression.IExpression;
import com.garganttua.core.supply.ISupplier;

/**
 * Expression context for parsing and evaluating expression strings.
 *
 * <p>An expression context is the main entry point for parsing expression strings into
 * evaluable expression objects. It maintains registries of available expression nodes
 * (functions) and leafs (terminals) that can be used in expressions, and provides
 * the parsing infrastructure to convert strings into expression trees.</p>
 *
 * <h2>Key Responsibilities</h2>
 * <ul>
 *   <li>Parse expression strings into {@code IExpression} objects</li>
 *   <li>Maintain registry of available expression nodes and leafs</li>
 *   <li>Provide type-safe expression evaluation</li>
 *   <li>Manage expression function lookup and resolution</li>
 * </ul>
 *
 * <h2>Usage Example (from ExpressionContextTest)</h2>
 * <pre>{@code
 * // Build expression context with functions
 * IExpressionContext context = new ExpressionContextBuilder()
 *     .withPackage("com.example.expressions")
 *     .withAutoDetection()
 *     .build();
 *
 * // Parse and evaluate simple expression
 * IExpression<String, ISupplier<String>> expr = context.expression("\"hello\"");
 * ISupplier<String> result = expr.evaluate();
 * assertEquals("hello", result.supply().get());
 *
 * // Parse and evaluate function call
 * IExpression<Integer, ISupplier<Integer>> addExpr = context.expression("add(10, 20)");
 * ISupplier<Integer> sum = addExpr.evaluate();
 * assertEquals(30, sum.supply().get());
 * }</pre>
 *
 * <h2>Expression Language</h2>
 * <p>The expression language supports:</p>
 * <ul>
 *   <li>Literals: strings, integers, booleans, class references</li>
 *   <li>Function calls: registered @ExpressionNode methods</li>
 *   <li>Nested expressions: functions can take expression results as parameters</li>
 *   <li>Type safety: expression types are validated at parse time</li>
 * </ul>
 *
 * <h2>Implementation</h2>
 * <p>The concrete implementation is provided by the garganttua-expression module,
 * which includes ANTLR4-based parsing and expression context management.</p>
 *
 * @see com.garganttua.core.expression.IExpression
 * @see com.garganttua.core.expression.IExpressionNode
 * @see com.garganttua.core.expression.dsl.IExpressionContextBuilder
 * @since 2.0.0-ALPHA01
 */
public interface IExpressionContext {

    /**
     * Parses an expression string into an evaluable expression object.
     *
     * <p>The expression string is parsed according to the expression grammar,
     * which includes support for literals, function calls, and nested expressions.
     * The returned expression can be evaluated multiple times.</p>
     *
     * @param expression the expression string to parse (e.g., "add(10, 20)" or "\"hello\"")
     * @return a parsed expression ready for evaluation
     * @throws com.garganttua.core.expression.ExpressionException if parsing fails
     */
    IExpression<?, ? extends ISupplier<?>> expression(String expression);

    /**
     * Returns the manual page documentation for a registered expression node.
     *
     * <p>The key should be in the format "functionName(Type1,Type2,...)" as returned
     * by {@link IExpressionNodeFactory#key()}.</p>
     *
     * @param key the unique key identifying the expression node factory
     * @return the manual page documentation, or null if the key is not found
     */
    String man(String key);

    /**
     * Returns a formatted list of all available expression node factories.
     *
     * <p>The list is formatted in man-style with each factory's key and description.</p>
     *
     * @return a formatted string containing all available expression factories
     */
    String man();

    /**
     * Returns the manual page documentation for a registered expression node by index.
     *
     * <p>The index corresponds to the position in the sorted list of factories,
     * as shown by {@link #listFactories()}. Index starts at 1.</p>
     *
     * @param index the 1-based index of the expression node factory
     * @return the manual page documentation, or null if the index is out of bounds
     */
    String man(int index);

}
