package com.garganttua.core.condition;

import com.garganttua.core.CoreException;

/**
 * Exception thrown when a condition evaluation fails or encounters an error.
 *
 * <p>
 * ConditionException is thrown when a condition cannot be evaluated due to invalid state,
 * missing required data, evaluation errors, or other exceptional circumstances. This is a
 * checked exception that forces callers to handle evaluation failures explicitly.
 * </p>
 *
 * <h2>Common Causes</h2>
 * <ul>
 *   <li>Missing required variables or context data</li>
 *   <li>Invalid supplier state (e.g., supplier returns null when non-null expected)</li>
 *   <li>Failed external service calls during evaluation</li>
 *   <li>Type conversion errors</li>
 *   <li>Malformed condition expressions</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public class UserConditions {
 *
 *     public static ICondition hasValidEmail(IObjectSupplier<User> userSupplier) {
 *         return () -> {
 *             User user = userSupplier.get();
 *             if (user == null) {
 *                 throw new ConditionException("User supplier returned null");
 *             }
 *             String email = user.getEmail();
 *             if (email == null) {
 *                 throw new ConditionException("User email is null");
 *             }
 *             return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
 *         };
 *     }
 * }
 *
 * // Handling the exception
 * try {
 *     boolean isValid = condition.evaluate();
 * } catch (ConditionException e) {
 *     logger.error("Failed to evaluate condition: " + e.getMessage());
 *     // Use default value or propagate error
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see ICondition
 * @see CoreException
 */
public class ConditionException extends CoreException {

    /**
     * Constructs a new ConditionException with the specified detail message.
     *
     * @param message the detail message explaining why the condition evaluation failed
     */
    public ConditionException(String message) {
        super(CoreException.CONDITION_ERROR, message);
    }

}
