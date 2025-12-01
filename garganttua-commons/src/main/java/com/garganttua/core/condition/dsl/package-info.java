/**
 * Fluent builder APIs for constructing complex boolean conditions.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides a fluent DSL interface for building complex boolean condition
 * expressions. It enables type-safe, composable condition construction for use in
 * runtime execution flows, validation rules, and business logic.
 * </p>
 *
 * <h2>Core Builder Interface</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.condition.dsl.IConditionBuilder} - Main condition builder</li>
 * </ul>
 *
 * <h2>Usage Example: Simple Conditions</h2>
 * <pre>{@code
 * // Build a simple condition
 * ICondition condition = new ConditionBuilder()
 *     .property("user.age")
 *     .greaterThan(18)
 *     .build();
 *
 * // Evaluate
 * boolean result = condition.evaluate(context);
 * }</pre>
 *
 * <h2>Usage Example: Composite Conditions</h2>
 * <pre>{@code
 * // Build complex AND condition
 * ICondition userCondition = new ConditionBuilder()
 *     .and()
 *         .property("user.age").greaterThan(18)
 *         .property("user.verified").equals(true)
 *         .property("user.status").equals("ACTIVE")
 *     .build();
 *
 * // Build complex OR condition
 * ICondition roleCondition = new ConditionBuilder()
 *     .or()
 *         .property("user.role").equals("ADMIN")
 *         .property("user.role").equals("MODERATOR")
 *         .property("user.permissions").contains("MANAGE_USERS")
 *     .build();
 * }</pre>
 *
 * <h2>Usage Example: Nested Conditions</h2>
 * <pre>{@code
 * // Build nested condition: (A AND B) OR (C AND D)
 * ICondition condition = new ConditionBuilder()
 *     .or()
 *         .and()
 *             .property("payment.type").equals("CREDIT_CARD")
 *             .property("payment.amount").lessThan(1000)
 *             .done()
 *         .and()
 *             .property("payment.type").equals("BANK_TRANSFER")
 *             .property("payment.verified").equals(true)
 *             .done()
 *     .build();
 * }</pre>
 *
 * <h2>Usage Example: Null Checks</h2>
 * <pre>{@code
 * // Check for null values
 * ICondition nullCheck = new ConditionBuilder()
 *     .property("user.email")
 *     .notNull()
 *     .build();
 *
 * ICondition isNull = new ConditionBuilder()
 *     .property("user.deletedAt")
 *     .isNull()
 *     .build();
 * }</pre>
 *
 * <h2>Usage Example: Custom Conditions</h2>
 * <pre>{@code
 * // Build custom predicate condition
 * ICondition customCondition = new ConditionBuilder()
 *     .custom(context -> {
 *         User user = context.get("user");
 *         return user.getAge() >= 18 && user.hasValidEmail();
 *     })
 *     .build();
 *
 * // Build custom extracted condition
 * ICondition extractedCondition = new ConditionBuilder()
 *     .customExtracted(
 *         context -> context.get("user"),
 *         user -> user.isActive() && !user.isBlocked()
 *     )
 *     .build();
 * }</pre>
 *
 * <h2>Supported Operations</h2>
 * <ul>
 *   <li><b>equals(Object)</b> - Equality check</li>
 *   <li><b>notEquals(Object)</b> - Inequality check</li>
 *   <li><b>isNull()</b> - Null check</li>
 *   <li><b>notNull()</b> - Not null check</li>
 *   <li><b>greaterThan(Comparable)</b> - Greater than comparison</li>
 *   <li><b>lessThan(Comparable)</b> - Less than comparison</li>
 *   <li><b>greaterOrEqual(Comparable)</b> - Greater or equal comparison</li>
 *   <li><b>lessOrEqual(Comparable)</b> - Less or equal comparison</li>
 *   <li><b>contains(Object)</b> - Collection/String contains</li>
 *   <li><b>matches(String)</b> - Regex pattern matching</li>
 * </ul>
 *
 * <h2>Logical Operators</h2>
 * <ul>
 *   <li><b>and()</b> - Logical AND (all conditions must be true)</li>
 *   <li><b>or()</b> - Logical OR (at least one condition must be true)</li>
 *   <li><b>not()</b> - Logical NOT (negates condition)</li>
 *   <li><b>xor()</b> - Logical XOR (exactly one condition must be true)</li>
 *   <li><b>nand()</b> - Logical NAND (NOT AND)</li>
 *   <li><b>nor()</b> - Logical NOR (NOT OR)</li>
 * </ul>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Fluent, chainable API</li>
 *   <li>Type-safe condition building</li>
 *   <li>Support for all boolean operators</li>
 *   <li>Nested condition support</li>
 *   <li>Property path resolution</li>
 *   <li>Custom predicate support</li>
 *   <li>Context-aware evaluation</li>
 *   <li>Reusable condition objects</li>
 * </ul>
 *
 * <h2>Integration</h2>
 * <p>
 * Conditions built with this DSL are used by:
 * </p>
 * <ul>
 *   <li>Runtime execution framework for conditional step execution</li>
 *   <li>Validation framework for business rules</li>
 *   <li>Workflow engines for branching logic</li>
 *   <li>Access control for permission checks</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.condition
 * @see com.garganttua.core.condition.dsl.IConditionBuilder
 * @see com.garganttua.core.runtime.annotations.Condition
 * @see com.garganttua.core.dsl.IAutomaticBuilder
 */
package com.garganttua.core.condition.dsl;
