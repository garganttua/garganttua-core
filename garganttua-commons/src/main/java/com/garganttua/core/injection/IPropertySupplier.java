package com.garganttua.core.injection;

import com.garganttua.core.supply.ISupplier;

/**
 * Represents a supplier capable of providing property values in the dependency injection system.
 *
 * <p>
 * An {@code IPropertySupplier} extends {@link ISupplier} to provide property values
 * with context-aware resolution. This interface is used internally by property providers
 * to lazily resolve and supply property values, enabling dynamic property resolution and
 * transformation.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // A property supplier provides values through the supply method
 * IPropertySupplier<String> supplier = ...;
 * IInjectionContext context = ...;
 *
 * // Supply a property value
 * String value = supplier.supply(context);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Thread safety is implementation-specific and depends on the property source.
 * </p>
 *
 * @param <Property> the type of property value this supplier provides
 * @since 2.0.0-ALPHA01
 * @see ISupplier
 * @see IPropertyProvider
 */
public interface IPropertySupplier<Property> extends ISupplier<Property>  {

}
