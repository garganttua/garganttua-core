package com.garganttua.core.injection;

import com.garganttua.core.reflection.binders.Dependent;
import com.garganttua.core.supply.IObjectSupplier;

/**
 * Represents a supplier capable of providing bean instances in the dependency injection system.
 *
 * <p>
 * An {@code IBeanSupplier} combines the functionality of {@link IObjectSupplier} for object
 * creation with {@link Dependent} to track dependencies. This interface is the base contract
 * for all bean suppliers, including factories and direct suppliers, enabling the DI framework
 * to instantiate and manage bean instances with proper dependency tracking.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // A bean supplier provides instances through the supply method
 * IBeanSupplier<MyService> supplier = ...;
 * IDiContext context = ...;
 *
 * // Supply a bean instance
 * MyService instance = supplier.supply(context);
 *
 * // Check dependencies for circular dependency detection
 * Set<Class<?>> dependencies = supplier.getDependencies();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Thread safety is implementation-specific and depends on the bean strategy (singleton vs prototype).
 * </p>
 *
 * @param <Bean> the type of bean this supplier provides
 * @since 2.0.0-ALPHA01
 * @see IObjectSupplier
 * @see Dependent
 * @see IBeanFactory
 */
public interface IBeanSupplier<Bean> extends IObjectSupplier<Bean>, Dependent {

}
