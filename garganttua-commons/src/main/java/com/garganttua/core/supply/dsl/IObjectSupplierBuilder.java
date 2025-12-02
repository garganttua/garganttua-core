package com.garganttua.core.supply.dsl;

import com.garganttua.core.dsl.IBuilder;
import com.garganttua.core.supply.IObjectSupplier;

/**
 * Base builder interface for constructing object supplier instances.
 *
 * <p>
 * {@code IObjectSupplierBuilder} provides the foundation for building both simple
 * and contextual suppliers through a fluent API. It extends {@link IBuilder} with
 * supplier-specific metadata methods that allow introspection of the supplier being
 * built before actual construction.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Building a simple supplier
 * IObjectSupplierBuilder<Logger, ?> builder = SupplierBuilder.forType(Logger.class);
 *
 * // Check properties before building
 * Class<?> type = builder.getSuppliedType();  // Returns Logger.class
 * boolean needsContext = builder.isContextual();  // Returns false
 *
 * // Complete the build
 * IObjectSupplier<Logger> supplier = builder
 *     .withValue(LoggerFactory.getLogger("app"))
 *     .build();
 * }</pre>
 *
 * <h2>Builder Metadata</h2>
 * <p>
 * This interface provides metadata methods that allow consumers to inspect
 * the supplier configuration during the build process, enabling conditional
 * logic and validation before the supplier is finalized.
 * </p>
 *
 * @param <Supplied> the type of object the built supplier will provide
 * @param <Built> the specific supplier type that will be constructed
 * @since 2.0.0-ALPHA01
 * @see IBuilder
 * @see ISupplierBuilder
 * @see IObjectSupplier
 */
public interface IObjectSupplierBuilder<Supplied, Built extends IObjectSupplier<Supplied>> extends IBuilder<Built> {

    /**
     * Returns the type of objects that the built supplier will provide.
     *
     * <p>
     * This method allows inspection of the supplier's target type before
     * construction is complete, enabling type-based logic and validation.
     * </p>
     *
     * @return the {@link Class} representing the type to be supplied
     */
    Class<Supplied> getSuppliedType();

    /**
     * Checks whether the built supplier will be contextual.
     *
     * <p>
     * Returns {@code true} if the supplier being built requires a context for
     * object creation (i.e., it will be an instance of {@link com.garganttua.core.supply.IContextualObjectSupplier}),
     * {@code false} for simple suppliers.
     * </p>
     *
     * @return {@code true} if the supplier requires context, {@code false} otherwise
     */
    boolean isContextual();

}
