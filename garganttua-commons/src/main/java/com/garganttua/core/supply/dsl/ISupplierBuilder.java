package com.garganttua.core.supply.dsl;

import java.lang.reflect.Type;

import com.garganttua.core.dsl.IBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.ISupplier;

/**
 * Base builder interface for constructing object supplier instances.
 *
 * <p>
 * {@code ISupplierBuilder} provides the foundation for building both
 * simple
 * and contextual suppliers through a fluent API. It extends {@link IBuilder}
 * with
 * supplier-specific metadata methods that allow introspection of the supplier
 * being
 * built before actual construction.
 * </p>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // Building a simple supplier
 * ISupplierBuilder<Logger, ?> builder = SupplierBuilder.forType(Logger.class);
 *
 * // Check properties before building
 * IClass<?> type = builder.getSuppliedClass();
 * boolean needsContext = builder.isContextual();
 *
 * // Complete the build
 * ISupplier<Logger> supplier = builder
 *         .withValue(LoggerFactory.getLogger("app"))
 *         .build();
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
 * @param <Built>    the specific supplier type that will be constructed
 * @since 2.0.0-ALPHA01
 * @see IBuilder
 * @see ISupplierBuilder
 * @see ISupplier
 */
public interface ISupplierBuilder<Supplied, Built extends ISupplier<Supplied>> extends IBuilder<Built> {

    /**
     * Returns the type of objects that the built supplier will provide.
     *
     * <p>
     * This method allows inspection of the supplier's target type before
     * construction is complete, enabling type-based logic and validation.
     * </p>
     *
     * @return the {@link IClass} representing the type to be supplied
     */
    IClass<Supplied> getSuppliedClass();

    Type getSuppliedType();

    /**
     * Checks whether the built supplier will be contextual.
     *
     * <p>
     * Returns {@code true} if the supplier being built requires a context for
     * object creation (i.e., it will be an instance of
     * {@link com.garganttua.core.supply.IContextualSupplier}),
     * {@code false} for simple suppliers.
     * </p>
     *
     * @return {@code true} if the supplier requires context, {@code false}
     *         otherwise
     */
    boolean isContextual();

}
