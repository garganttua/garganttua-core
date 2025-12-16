/**
 * Fluent builder API implementations for constructing boolean conditions.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides concrete implementations of the fluent DSL interfaces defined
 * in garganttua-commons for building conditions. It implements builder classes for
 * all condition types, enabling type-safe, composable condition construction.
 * </p>
 *
 * <h2>Implementation Classes</h2>
 * <ul>
 *   <li>{@code EqualsConditionBuilder} - Builds equality conditions</li>
 *   <li>{@code NotEqualsConditionBuilder} - Builds inequality conditions</li>
 *   <li>{@code NullConditionBuilder} - Builds null check conditions</li>
 *   <li>{@code NotNullConditionBuilder} - Builds not-null check conditions</li>
 *   <li>{@code CustomConditionBuilder} - Builds custom predicate conditions</li>
 *   <li>{@code CustomExtractedConditionBuilder} - Builds custom extracted conditions</li>
 * </ul>
 *
 * <h2>Logical Operator Builders</h2>
 * <ul>
 *   <li>{@code AndConditionBuilder} - Builds AND conditions</li>
 *   <li>{@code OrConditionBuilder} - Builds OR conditions</li>
 *   <li>{@code NandConditionBuilder} - Builds NAND conditions</li>
 *   <li>{@code NorConditionBuilder} - Builds NOR conditions</li>
 *   <li>{@code XorConditionBuilder} - Builds XOR conditions</li>
 * </ul>
 *
 * <h2>Usage Example: Equality Checks (from ConditionTest)</h2>
 * <pre>{@code
 * import static com.garganttua.core.condition.Conditions.*;
 * import static com.garganttua.core.supply.dsl.FixedSupplierBuilder.*;
 *
 * // Equals condition
 * Conditions.equals(of(10), of(10)).build().fullEvaluate(); // true
 * Conditions.equals(of("abc"), of("abc")).build().fullEvaluate(); // true
 * Conditions.equals(of("abc"), of("ABC")).build().fullEvaluate(); // false
 *
 * // Not equals condition
 * notEquals(of(10), of(20)).build().fullEvaluate(); // true
 * notEquals(of("abc"), of("ABC")).build().fullEvaluate(); // true
 *
 * // Type mismatch detection
 * Conditions.equals(of(10), of(10.0)).build().fullEvaluate(); // throws DslException
 * }</pre>
 *
 * <h2>Usage Example: AND Operator (from ConditionTest)</h2>
 * <pre>{@code
 * import static com.garganttua.core.condition.Conditions.*;
 *
 * // AND - all conditions must be true
 * and(
 *     custom(of(10), v -> v > 5),
 *     custom(of(20), v -> v < 30)
 * ).build().fullEvaluate(); // true
 *
 * and(
 *     isNull(NullSupplierBuilder.of(String.class)),
 *     isNull(NullSupplierBuilder.of(String.class))
 * ).build().fullEvaluate(); // true
 *
 * and(
 *     isNull(NullSupplierBuilder.of(String.class)),
 *     isNull(of("null"))
 * ).build().fullEvaluate(); // false
 * }</pre>
 *
 * <h2>Usage Example: OR Operator (from ConditionTest)</h2>
 * <pre>{@code
 * // OR - at least one condition must be true
 * or(
 *     custom(of(5), v -> v > 3),
 *     custom(of(2), v -> v > 10)
 * ).build().fullEvaluate(); // true (first is true)
 *
 * or(
 *     custom(of(1), v -> v > 3),
 *     custom(of(2), v -> v > 10)
 * ).build().fullEvaluate(); // false (both are false)
 * }</pre>
 *
 * <h2>Usage Example: XOR Operator (from ConditionTest)</h2>
 * <pre>{@code
 * // XOR - odd number of conditions must be true
 * xor(
 *     custom(of(10), v -> v > 5),    // true
 *     custom(of(50), v -> v < 30)    // false
 * ).build().fullEvaluate(); // true
 *
 * xor(
 *     custom(of(10), v -> v > 5),    // true
 *     custom(of(20), v -> v < 30)    // true
 * ).build().fullEvaluate(); // false
 * }</pre>
 *
 * <h2>Usage Example: Custom Conditions (from ConditionTest)</h2>
 * <pre>{@code
 * // Direct predicate
 * custom(of(125), val -> val > 3).build().fullEvaluate(); // true
 * custom(of(true), val -> val).build().fullEvaluate(); // true
 *
 * // Extracted predicate - extract property then test
 * custom(of("hello"), String::length, len -> len > 3).build().fullEvaluate(); // true
 * custom(of("abc"), String::isEmpty, empty -> !empty).build().fullEvaluate(); // true
 * custom(of(""), String::isEmpty, empty -> !empty).build().fullEvaluate(); // false
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
 *   <li>Fluent, chainable API</li>
 *   <li>Type-safe condition building</li>
 *   <li>Support for all condition types</li>
 *   <li>Nested condition support</li>
 *   <li>Custom predicate support</li>
 *   <li>Value extraction configuration</li>
 *   <li>Clear builder hierarchy</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.condition.dsl
 * @see com.garganttua.core.condition
 */
package com.garganttua.core.condition.dsl;
