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
 * <h2>Usage Example: Simple Conditions</h2>
 * <pre>{@code
 * // Equals condition
 * ICondition ageCheck = Conditions.equals(
 *     context -> context.get("age"),
 *     18
 * );
 * boolean result = ageCheck.evaluate(context);
 *
 * // Null check
 * ICondition emailCheck = Conditions.notNull(
 *     context -> context.get("email")
 * );
 * }</pre>
 *
 * <h2>Usage Example: Composite Conditions</h2>
 * <pre>{@code
 * // AND condition
 * ICondition userCondition = Conditions.and(
 *     Conditions.equals(ctx -> ctx.get("status"), "ACTIVE"),
 *     Conditions.notNull(ctx -> ctx.get("email")),
 *     Conditions.equals(ctx -> ctx.get("verified"), true)
 * );
 *
 * // OR condition
 * ICondition roleCondition = Conditions.or(
 *     Conditions.equals(ctx -> ctx.get("role"), "ADMIN"),
 *     Conditions.equals(ctx -> ctx.get("role"), "MODERATOR")
 * );
 * }</pre>
 *
 * <h2>Usage Example: Custom Conditions</h2>
 * <pre>{@code
 * // Custom predicate condition
 * ICondition customCheck = Conditions.custom(context -> {
 *     User user = context.get("user");
 *     return user.getAge() >= 18 && user.hasValidEmail();
 * });
 *
 * // Custom extracted condition
 * ICondition extractedCheck = Conditions.customExtracted(
 *     context -> context.get("user"),
 *     user -> user.isActive() && !user.isBlocked()
 * );
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
