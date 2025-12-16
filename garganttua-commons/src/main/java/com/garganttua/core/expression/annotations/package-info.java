/**
 * Annotations for declarative expression definition and registration.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides annotations for marking methods and classes as expression
 * components that can be automatically discovered and registered in expression contexts.
 * </p>
 *
 * <h2>Annotations</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.expression.annotations.Expression} - Marks a class as an expression</li>
 *   <li>{@link com.garganttua.core.expression.annotations.ExpressionNode} - Marks a static method as an expression node factory</li>
 *   <li>{@link com.garganttua.core.expression.annotations.ExpressionLeaf} - Marks a static method as an expression leaf factory</li>
 * </ul>
 *
 * <h2>Usage Pattern</h2>
 * <p>
 * These annotations are used to create custom expression DSLs by annotating
 * static factory methods that create expression nodes and leafs. The expression
 * context builder can then automatically discover these methods using reflection
 * or annotation scanning.
 * </p>
 *
 * <h2>Example (from ExpressionContextTest)</h2>
 * <pre>{@code
 * public class CustomExpressions {
 *
 *     @ExpressionLeaf
 *     public static ISupplier<Integer> value(int val) {
 *         return () -> Optional.of(val);
 *     }
 *
 *     @ExpressionNode
 *     public static ISupplier<Integer> add(ISupplier<Integer> a, ISupplier<Integer> b) {
 *         return () -> Optional.of(a.supply().get() + b.supply().get());
 *     }
 * }
 * }</pre>
 *
 * <h2>Related Packages</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.expression} - Core expression interfaces</li>
 *   <li>{@link com.garganttua.core.expression.context} - Expression context management</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 */
package com.garganttua.core.expression.annotations;
