package com.garganttua.core.supply;

import java.util.Optional;

/**
 * Functional interface for context-aware object creation logic.
 *
 * <p>
 * {@code IContextualObjectSupply} provides a lightweight, functional approach to
 * defining context-dependent object creation logic. It is typically used as a lambda
 * or method reference when building {@link IContextualObjectSupplier} instances through
 * builder APIs. Unlike {@link IContextualObjectSupplier}, this interface focuses solely
 * on the supply logic without metadata like type information.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Using as a lambda in a builder
 * IObjectSupplier<Logger> loggerSupplier = SupplierBuilder
 *     .forType(Logger.class)
 *     .withContext(DiContext.class, (context, others) -> {
 *         String appName = context.getProperty("app.name");
 *         return Optional.of(LoggerFactory.getLogger(appName));
 *     })
 *     .build();
 *
 * // Using as a method reference
 * IObjectSupplier<Database> dbSupplier = SupplierBuilder
 *     .forType(Database.class)
 *     .withContext(Config.class, this::createDatabase)
 *     .build();
 *
 * private Optional<Database> createDatabase(Config config, Object... others) {
 *     return Optional.of(new Database(config.getDbUrl()));
 * }
 * }</pre>
 *
 * <h2>Comparison with IContextualObjectSupplier</h2>
 * <ul>
 *   <li>{@code IContextualObjectSupply}: Pure creation logic (functional interface)</li>
 *   <li>{@link IContextualObjectSupplier}: Full supplier with metadata and lifecycle</li>
 * </ul>
 *
 * @param <Supplied> the type of object this supply logic creates
 * @param <Context> the type of the primary context required for creation
 * @since 2.0.0-ALPHA01
 * @see IContextualObjectSupplier
 * @see com.garganttua.core.supply.dsl.ISupplierBuilder#withContext(Class, IContextualObjectSupply)
 */
@FunctionalInterface
public interface IContextualObjectSupply<Supplied, Context> {

    /**
     * Creates an object instance using the provided context.
     *
     * <p>
     * This method implements the actual object creation logic, using the provided
     * context and any additional contexts to create or retrieve an instance. The
     * returned {@link Optional} should be empty only if the object genuinely cannot
     * be created (not for error conditions, which should throw exceptions).
     * </p>
     *
     * @param context the primary context required for object creation (never {@code null})
     * @param otherContexts additional optional contexts that may assist in object creation
     * @return an {@link Optional} containing the created instance, or empty if unavailable
     */
    Optional<Supplied> supplyObject(Context context, Object... otherContexts);

}
