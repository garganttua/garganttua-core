/**
 * Built-in expression functions for common operations.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides the framework's built-in expression functions: type parsing
 * and conversion, string manipulation, and simple arithmetic. They are exposed as
 * {@link com.garganttua.core.expression.annotations.Expression}-annotated static methods
 * so the expression context can discover and register them by name.
 * </p>
 *
 * <h2>Built-in Functions</h2>
 * <p>
 * The {@link com.garganttua.core.expression.functions.Expressions} class provides:
 * </p>
 * <ul>
 *   <li>Primitive parsers ({@code int}, {@code long}, {@code double}, {@code boolean}, ...)</li>
 *   <li>String conversion ({@code string}) and concatenation ({@code concatenate})</li>
 *   <li>Class loading by name ({@code class})</li>
 *   <li>Integer arithmetic ({@code increment}, {@code decrement})</li>
 * </ul>
 *
 * <h2>Usage Example (from ExpressionContextTest)</h2>
 * <pre>{@code
 * // String expression
 * ISupplier<String> result = context.parse("\"hello\"", String.class);
 * assertEquals("hello", result.supply().get());
 *
 * // Integer expression
 * ISupplier<Integer> num = context.parse("42", Integer.class);
 * assertEquals(42, num.supply().get());
 *
 * // Type expression
 * ISupplier<Class<?>> type = context.parse("java.lang.String", Class.class);
 * assertEquals(String.class, type.supply().get());
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Standard primitive types</li>
 *   <li>Annotation-based discovery</li>
 *   <li>Integration with expression context</li>
 * </ul>
 *
 * <h2>Related Packages</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.expression} - Core expression interfaces</li>
 *   <li>{@link com.garganttua.core.expression.context} - Expression context management</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 */
package com.garganttua.core.expression.functions;
