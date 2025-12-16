/**
 * Boolean condition evaluation framework implementation.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides the concrete implementation of the condition evaluation framework.
 * It implements various condition types (equals, null checks, logical operators) and
 * provides a unified API for evaluating complex boolean expressions.
 * </p>
 *
 * <h2>Main Implementation Classes</h2>
 * <ul>
 *   <li>{@code Conditions} - Main condition factory and evaluation utilities</li>
 *   <li>{@code EqualsCondition} - Equality comparison condition</li>
 *   <li>{@code NotEqualsCondition} - Inequality comparison condition</li>
 *   <li>{@code NullCondition} - Null check condition</li>
 *   <li>{@code NotNullCondition} - Not null check condition</li>
 *   <li>{@code CustomCondition} - Custom predicate-based condition</li>
 *   <li>{@code CustomExtractedCondition} - Custom condition with value extraction</li>
 * </ul>
 *
 * <h2>Logical Operator Classes</h2>
 * <ul>
 *   <li>{@code AndCondition} - Logical AND (all must be true)</li>
 *   <li>{@code OrCondition} - Logical OR (at least one must be true)</li>
 *   <li>{@code NandCondition} - Logical NAND (NOT AND)</li>
 *   <li>{@code NorCondition} - Logical NOR (NOT OR)</li>
 *   <li>{@code XorCondition} - Logical XOR (exactly one must be true)</li>
 * </ul>
 *
 * <h2>Usage Example: Null Checks</h2>
 * <pre>{@code
 * import static com.garganttua.core.condition.Conditions.*;
 * import static com.garganttua.core.supply.dsl.FixedSupplierBuilder.*;
 *
 * // Check if value is null
 * isNull(of("null")).build().fullEvaluate(); // false
 * isNull(NullSupplierBuilder.of(String.class)).build().fullEvaluate(); // true
 *
 * // Check if value is not null
 * isNotNull(of("hello")).build().fullEvaluate(); // true
 * isNotNull(NullSupplierBuilder.of(String.class)).build().fullEvaluate(); // false
 * }</pre>
 *
 * <h2>Usage Example: Equality Checks</h2>
 * <pre>{@code
 * // Equals - compares two supplied values
 * Conditions.equals(of(10), of(10)).build().fullEvaluate(); // true
 * Conditions.equals(of("abc"), of("ABC")).build().fullEvaluate(); // false
 *
 * // NotEquals
 * notEquals(of(10), of(20)).build().fullEvaluate(); // true
 *
 * // Type safety - throws DslException for type mismatch
 * Conditions.equals(of(10), of(10.0)).build().fullEvaluate(); // throws DslException
 * }</pre>
 *
 * <h2>Usage Example: Logical Operators</h2>
 * <pre>{@code
 * // AND - all conditions must be true
 * and(
 *     custom(of(10), v -> v > 5),
 *     custom(of(20), v -> v < 30)
 * ).build().fullEvaluate(); // true
 *
 * // OR - at least one condition must be true
 * or(
 *     custom(of(5), v -> v > 3),
 *     custom(of(2), v -> v > 10)
 * ).build().fullEvaluate(); // true
 *
 * // XOR - odd number of conditions must be true
 * xor(
 *     custom(of(10), v -> v > 5),
 *     custom(of(50), v -> v < 30)
 * ).build().fullEvaluate(); // true
 *
 * // NAND - NOT(AND)
 * nand(
 *     custom(of(10), v -> v > 5),
 *     custom(of(50), v -> v < 30)
 * ).build().fullEvaluate(); // true
 *
 * // NOR - NOT(OR)
 * nor(
 *     custom(of(1), v -> v > 3),
 *     custom(of(2), v -> v > 3)
 * ).build().fullEvaluate(); // true
 * }</pre>
 *
 * <h2>Usage Example: Custom Conditions</h2>
 * <pre>{@code
 * // Direct predicate
 * custom(of(125), val -> val > 3).build().fullEvaluate(); // true
 * custom(of(true), val -> val).build().fullEvaluate(); // true
 *
 * // Extracted predicate - extract property then test
 * custom(of("hello"), String::length, len -> len > 3).build().fullEvaluate(); // true
 * custom(of("abc"), String::isEmpty, empty -> !empty).build().fullEvaluate(); // true
 *
 * // Computed value extraction
 * custom(of("hello"), str -> str.chars().sum(), sum -> sum > 500).build().fullEvaluate(); // true
 *
 * // Identity extraction
 * custom(of("identity"), Function.identity(), s -> s.startsWith("i")).build().fullEvaluate(); // true
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Type-safe condition evaluation</li>
 *   <li>Equality and inequality checks</li>
 *   <li>Null safety checks</li>
 *   <li>Logical operators (AND, OR, NAND, NOR, XOR)</li>
 *   <li>Custom predicate support</li>
 *   <li>Value extraction before evaluation</li>
 *   <li>Context-aware evaluation</li>
 *   <li>Composable conditions</li>
 *   <li>Reusable condition objects</li>
 * </ul>
 *
 * <h2>Related Packages</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.condition.dsl} - Fluent builder implementations</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.condition.dsl
 * @see com.garganttua.core.runtime.annotations.Condition
 */
package com.garganttua.core.condition;
