/**
 * Expressive DSL for defining, combining, and evaluating runtime conditions.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides a powerful condition evaluation framework with a fluent DSL for
 * building complex boolean expressions. It supports full boolean algebra (AND, OR, XOR, NAND, NOR),
 * custom predicates, and property extraction from objects.
 * </p>
 *
 * <h2>Core Interfaces</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.condition.ICondition} - Base condition interface</li>
 *   <li>{@link com.garganttua.core.condition.IConditionEvaluator} - Evaluates conditions</li>
 *   <li>{@link com.garganttua.core.condition.IConditionBuilder} - Builder for condition composition</li>
 * </ul>
 *
 * <h2>Condition Types</h2>
 * <ul>
 *   <li><b>Simple Conditions</b> - isNull, isNotNull, equals, notEquals</li>
 *   <li><b>Logical Operators</b> - AND, OR, XOR, NAND, NOR, NOT</li>
 *   <li><b>Custom Predicates</b> - User-defined validation logic</li>
 *   <li><b>Property Conditions</b> - Extract and test object properties</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Simple Conditions</h3>
 * <pre>{@code
 * IObjectSupplier<User> userSupplier = () -> currentUser;
 *
 * // Check if user is not null
 * ICondition userExists = isNotNull(userSupplier).build();
 * boolean exists = userExists.evaluate(); // true if user != null
 *
 * // Check equality
 * ICondition isAdmin = equals(userSupplier, () -> "admin").build();
 * }</pre>
 *
 * <h3>Logical Combinations</h3>
 * <pre>{@code
 * // Complex business rule: user is eligible if adult AND (premium OR old account)
 * ICondition eligibility = and(
 *     isNotNull(userSupplier),
 *     custom(userSupplier, User::getAge, age -> age >= 18),
 *     or(
 *         custom(userSupplier, User::isPremium, premium -> premium),
 *         custom(userSupplier, User::getAccountAge, age -> age > 365)
 *     )
 * ).build();
 *
 * boolean isEligible = eligibility.evaluate();
 * }</pre>
 *
 * <h3>Property Extraction</h3>
 * <pre>{@code
 * // Check if user's country is US
 * ICondition isUSUser = custom(
 *     userSupplier,
 *     User::getCountry,
 *     country -> "US".equals(country)
 * ).build();
 *
 * // Check if order total is above threshold
 * ICondition largeOrder = custom(
 *     orderSupplier,
 *     Order::getTotal,
 *     total -> total.compareTo(new BigDecimal("1000")) > 0
 * ).build();
 * }</pre>
 *
 * <h3>Negation</h3>
 * <pre>{@code
 * ICondition isNotAnonymous = not(
 *     isNull(userSupplier)
 * ).build();
 *
 * // NAND - not (A and B)
 * ICondition condition = nand(
 *     isNotNull(userSupplier),
 *     custom(userSupplier, User::isBanned, banned -> banned)
 * ).build();
 * }</pre>
 *
 * <h2>Integration with Runtime</h2>
 * <pre>{@code
 * @RuntimeDefinition
 * public class ConditionalRuntime {
 *
 *     @Step(
 *         method = "processVipOrder",
 *         skipCondition = "isNotVip"
 *     )
 *     public void processVipOrder(@Input Order order) {
 *         // Only executed if user is VIP
 *     }
 *
 *     public ICondition isNotVip(@Input Order order) {
 *         return not(custom(
 *             () -> order.getUser(),
 *             User::isVip,
 *             vip -> vip
 *         )).build();
 *     }
 * }
 * }</pre>
 *
 * <h2>Benefits</h2>
 * <ul>
 *   <li><b>Declarative</b> - Express business rules clearly</li>
 *   <li><b>Composable</b> - Combine simple conditions into complex logic</li>
 *   <li><b>Reusable</b> - Store and reuse condition definitions</li>
 *   <li><b>Testable</b> - Easy to unit test conditions</li>
 *   <li><b>Type-safe</b> - Compile-time checking with generics</li>
 * </ul>
 *
 * <h2>Related Packages</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.condition.dsl} - Fluent builder APIs for condition creation</li>
 *   <li>{@link com.garganttua.core.runtime} - Integration with runtime workflows</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.condition.ICondition
 * @see com.garganttua.core.condition.dsl
 */
package com.garganttua.core.condition;
