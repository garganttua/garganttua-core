package com.garganttua.core.condition;

import java.lang.reflect.Type;

import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.IExpressionNode;
import com.garganttua.core.supply.ISupplier;

/**
 * Represents a boolean condition that can be evaluated to true or false.
 *
 * <p>
 * ICondition is a functional interface that encapsulates boolean logic for
 * runtime decision-making.
 * Conditions can be simple predicates or complex composite expressions built
 * using boolean algebra
 * (AND, OR, XOR, NOT, NAND, NOR). They are commonly used in runtime workflows
 * to control step execution,
 * validate inputs, or implement business rules.
 * </p>
 *
 * <h2>Core Use Cases</h2>
 * <ul>
 * <li><b>Step Guards</b> - Control whether a runtime step should execute</li>
 * <li><b>Business Rules</b> - Encode complex validation and eligibility
 * logic</li>
 * <li><b>Feature Toggles</b> - Enable/disable features based on runtime
 * state</li>
 * <li><b>Access Control</b> - Implement permission checks and authorization
 * rules</li>
 * </ul>
 *
 * <h2>Usage Example - Simple Condition</h2>
 * 
 * <pre>{@code
 * // Lambda-based condition
 * ICondition isAdult = () -> user.getAge() >= 18;
 *
 * if (isAdult.evaluate()) {
 *     // Allow access
 * }
 * }</pre>
 *
 * <h2>Usage Example - Composite Conditions</h2>
 * 
 * <pre>{@code
 * import static com.garganttua.core.condition.Conditions.*;
 *
 * // Complex business rule: eligible if adult AND (premium OR long-time user)
 * ICondition eligibility = and(
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
 * <h2>Usage Example - Runtime Integration</h2>
 * 
 * <pre>
 * {
 *     &#64;code
 *     &#64;RuntimeDefinition
 *     public class ConditionalRuntime {
 *
 *         @Operation
 *         &#64;Condition("isVipUser")
 *         public void processVipDiscount(@Input Order order) {
 *             // Only executes if user is VIP
 *             order.applyDiscount(0.20);
 *         }
 *
 *         public ICondition isVipUser(@Input Order order) {
 *             return () -> order.getUser().isVip();
 *         }
 *     }
 * }
 * </pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Condition implementations should be stateless and thread-safe. Multiple
 * threads may call
 * {@link #evaluate()} concurrently. If a condition depends on external state,
 * ensure proper
 * synchronization or use immutable suppliers.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.condition.dsl.IConditionBuilder
 * @see com.garganttua.core.runtime.annotations.Condition
 */
// @FunctionalInterface
public interface ICondition extends IExpressionNode<Boolean, ISupplier<Boolean>> {

    @SuppressWarnings("unchecked")
    @Override
    default Type getSuppliedType() {
        return (Class<ISupplier<Boolean>>) (Class<?>) ISupplier.class;
    }

    default Boolean fullEvaluate() throws ExpressionException {
        return this.evaluate().supply().get();
    }

    @Override
    default public Class<Boolean> getFinalSuppliedClass() {
        return Boolean.class;
    }

}
