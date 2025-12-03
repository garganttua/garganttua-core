package com.garganttua.core.supply;

import com.garganttua.core.CoreException;

import lombok.extern.slf4j.Slf4j;

/**
 * Exception thrown when errors occur during object supply operations.
 *
 * <p>
 * {@code SupplyException} is thrown by {@link IObjectSupplier} and
 * {@link IContextualObjectSupplier} implementations when object creation or
 * retrieval fails. This includes scenarios such as missing contexts, invalid
 * configuration, instantiation failures, or resource unavailability.
 * </p>
 *
 * <h2>Common Scenarios</h2>
 * <ul>
 *   <li>Required context not provided or incompatible</li>
 *   <li>Instantiation failure (constructor exceptions, reflection errors)</li>
 *   <li>Missing dependencies or resources</li>
 *   <li>Configuration errors preventing object creation</li>
 *   <li>Runtime errors during object initialization</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * IObjectSupplier<Database> supplier = ...;
 * try {
 *     Database db = Supplier.contextualSupply(supplier, contexts);
 * } catch (SupplyException e) {
 *     // Handle supply failure
 *     logger.error("Failed to supply database: " + e.getMessage(), e);
 *     // Fallback or error recovery
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see CoreException
 * @see IObjectSupplier
 * @see IContextualObjectSupplier
 */
@Slf4j
public class SupplyException extends CoreException {

    /**
     * Constructs a new supply exception wrapping another exception.
     *
     * @param e the underlying exception that caused the supply failure
     */
    public SupplyException(Exception e) {
        super(CoreException.SUPPLY_ERROR, e);
        log.atTrace().log("Exiting SupplyException constructor");
    }

    /**
     * Constructs a new supply exception with the specified message.
     *
     * @param message the detailed error message describing why supply failed
     */
    public SupplyException(String message) {
        super(CoreException.SUPPLY_ERROR, message);
        log.atTrace().log("Exiting SupplyException constructor");
    }

}
