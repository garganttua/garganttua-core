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
 * <h2>Usage Example: Simple Condition Builders</h2>
 * <pre>{@code
 * // Build equals condition
 * ICondition ageCheck = new EqualsConditionBuilder()
 *     .extractor(context -> context.get("age"))
 *     .expectedValue(18)
 *     .build();
 *
 * // Build not null condition
 * ICondition emailCheck = new NotNullConditionBuilder()
 *     .extractor(context -> context.get("email"))
 *     .build();
 * }</pre>
 *
 * <h2>Usage Example: Composite Condition Builders</h2>
 * <pre>{@code
 * // Build AND condition
 * ICondition userCondition = new AndConditionBuilder()
 *     .add(new EqualsConditionBuilder()
 *         .extractor(ctx -> ctx.get("status"))
 *         .expectedValue("ACTIVE")
 *         .build())
 *     .add(new NotNullConditionBuilder()
 *         .extractor(ctx -> ctx.get("email"))
 *         .build())
 *     .build();
 *
 * // Build OR condition
 * ICondition roleCondition = new OrConditionBuilder()
 *     .add(new EqualsConditionBuilder()
 *         .extractor(ctx -> ctx.get("role"))
 *         .expectedValue("ADMIN")
 *         .build())
 *     .add(new EqualsConditionBuilder()
 *         .extractor(ctx -> ctx.get("role"))
 *         .expectedValue("MODERATOR")
 *         .build())
 *     .build();
 * }</pre>
 *
 * <h2>Usage Example: Custom Condition Builders</h2>
 * <pre>{@code
 * // Build custom condition
 * ICondition customCheck = new CustomConditionBuilder()
 *     .predicate(context -> {
 *         User user = context.get("user");
 *         return user.getAge() >= 18 && user.hasValidEmail();
 *     })
 *     .build();
 *
 * // Build custom extracted condition
 * ICondition extractedCheck = new CustomExtractedConditionBuilder()
 *     .extractor(context -> context.get("user"))
 *     .predicate(user -> user.isActive() && !user.isBlocked())
 *     .build();
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
