/**
 * Standard expression functions and leafs for common operations.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides a collection of standard expression functions and leafs
 * that can be used in expression evaluation. These include basic type operations,
 * string manipulation, and primitive value creation.
 * </p>
 *
 * <h2>Standard Functions</h2>
 * <p>
 * The {@code StandardExpressionLeafs} class provides annotated static methods
 * for common expression operations:
 * </p>
 * <ul>
 *   <li>String literals and operations</li>
 *   <li>Integer literals and arithmetic</li>
 *   <li>Boolean values</li>
 *   <li>Type references (Class objects)</li>
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
 *   <li>Type-safe leaf creation</li>
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
