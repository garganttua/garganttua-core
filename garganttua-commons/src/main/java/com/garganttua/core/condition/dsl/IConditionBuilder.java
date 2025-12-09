package com.garganttua.core.condition.dsl;

import java.lang.reflect.Type;

import com.garganttua.core.condition.ICondition;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

/**
 * Builder interface for constructing condition expressions using a fluent DSL.
 *
 * <p>
 * IConditionBuilder provides a type-safe, chainable API for creating complex
 * boolean conditions
 * using boolean algebra operators (AND, OR, XOR, NOT, NAND, NOR) and custom
 * predicates. The builder
 * pattern enables readable, declarative condition definitions without nested
 * constructor calls.
 * </p>
 *
 * <h2>Core Features</h2>
 * <ul>
 * <li><b>Boolean Operators</b> - Combine conditions with and(), or(), xor(),
 * not(), nand(), nor()</li>
 * <li><b>Null Checks</b> - isNull(), isNotNull() for object existence
 * validation</li>
 * <li><b>Equality</b> - equals(), notEquals() for value comparison</li>
 * <li><b>Custom Predicates</b> - custom() for arbitrary boolean logic with
 * property extraction</li>
 * <li><b>Type Safety</b> - Generics ensure compile-time type checking</li>
 * </ul>
 *
 * <h2>Usage Example - Simple Conditions</h2>
 * 
 * <pre>{@code
 * import static com.garganttua.core.condition.Conditions.*;
 *
 * ISupplier<User> userSupplier = () -> getCurrentUser();
 *
 * // Check if user exists
 * ICondition userExists = isNotNull(userSupplier).build();
 *
 * // Check if user is admin
 * ICondition isAdmin = equals(
 *     userSupplier,
 *     () -> "ADMIN"
 * ).build();
 * }</pre>
 *
 * <h2>Usage Example - Composite Conditions</h2>
 * 
 * <pre>{@code
 * // Complex eligibility rule: adult AND (premium OR veteran user)
 * ICondition eligibility = and(
 *         // User must exist
 *         isNotNull(userSupplier),
 *         // User must be adult
 *         custom(userSupplier, User::getAge, age -> age >= 18),
 *         // User must be either premium or veteran
 *         or(
 *                 custom(userSupplier, User::isPremium, premium -> premium),
 *                 custom(userSupplier, User::getAccountAge, age -> age > 365)))
 *         .build();
 *
 * boolean canAccess = eligibility.evaluate();
 * }</pre>
 *
 * <h2>Usage Example - Property Extraction</h2>
 * 
 * <pre>{@code
 * ICondition isUSUser = custom(
 *         userSupplier,
 *         User::getAddress, // Extract address
 *         Address::getCountry, // Extract country from address
 *         country -> "US".equals(country) // Test condition
 * ).build();
 *
 * ICondition largeOrder = custom(
 *         orderSupplier,
 *         Order::getTotal,
 *         total -> total.compareTo(new BigDecimal("1000")) > 0).build();
 * }</pre>
 *
 * <h2>Usage Example - Logical Operators</h2>
 * 
 * <pre>{@code
 * // NAND: NOT (banned AND suspended)
 * ICondition canLogin = nand(
 *         custom(userSupplier, User::isBanned, banned -> banned),
 *         custom(userSupplier, User::isSuspended, suspended -> suspended)).build();
 *
 * // XOR: premium OR trial, but not both
 * ICondition hasExactlyOneAccess = xor(
 *         custom(userSupplier, User::isPremium, p -> p),
 *         custom(userSupplier, User::isTrialActive, t -> t)).build();
 * }</pre>
 *
 * <h2>Integration with Runtime</h2>
 * <p>
 * Conditions built with IConditionBuilder can be used in runtime workflows to
 * control
 * step execution:
 * </p>
 * 
 * <pre>
 * {
 *     &#64;code
 *     &#64;RuntimeDefinition
 *     public class ConditionalRuntime {
 *
 *         @Operation
 *         &#64;Condition("shouldProcess")
 *         public void processOrder(@Input Order order) {
 *             // Only executes if shouldProcess evaluates to true
 *         }
 *
 *         public ICondition shouldProcess(@Input Order order) {
 *             return and(
 *                     isNotNull(() -> order),
 *                     custom(() -> order, Order::getStatus, status -> "PENDING".equals(status))).build();
 *         }
 *     }
 * }
 * </pre>
 *
 * @since 2.0.0-ALPHA01
 * @see ICondition
 * @see com.garganttua.core.dsl.IBuilder
 * @see com.garganttua.core.supply.ISupplier
 */
public interface IConditionBuilder extends ISupplierBuilder<ISupplier<Boolean>, ICondition> {

    @SuppressWarnings("unchecked")
    @Override
    default Type getSuppliedType() {
        return (Class<ISupplier<Boolean>>) (Class<?>) ISupplier.class;
    }

    @Override
    default boolean isContextual() {
        return false;
    }
}
