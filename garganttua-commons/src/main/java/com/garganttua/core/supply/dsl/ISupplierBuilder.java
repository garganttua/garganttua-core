package com.garganttua.core.supply.dsl;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.binders.IConstructorBinder;
import com.garganttua.core.supply.IContextualObjectSupply;
import com.garganttua.core.supply.IObjectSupplier;

/**
 * Builder interface for constructing object supplier instances with various supply strategies.
 *
 * <p>
 * {@code ISupplierBuilder} provides a fluent API for building {@link IObjectSupplier} instances
 * with different supply strategies: fixed values, constructor-based instantiation, or
 * context-aware creation. The builder supports both nullable and non-nullable suppliers,
 * and automatically creates the appropriate supplier type (simple or contextual) based on
 * the configuration.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Building a simple value-based supplier
 * IObjectSupplier<Config> configSupplier = SupplierBuilder
 *     .forType(Config.class)
 *     .withValue(new Config("default"))
 *     .build();
 *
 * // Building a constructor-based supplier
 * IObjectSupplier<Database> dbSupplier = SupplierBuilder
 *     .forType(Database.class)
 *     .withConstructor(ConstructorBinder.forClass(Database.class)
 *         .withParameter(String.class, "jdbc:mysql://localhost:3306/db")
 *         .build())
 *     .build();
 *
 * // Building a contextual supplier with nullable support
 * IObjectSupplier<UserSession> sessionSupplier = SupplierBuilder
 *     .forType(UserSession.class)
 *     .nullable(true)  // Can return empty Optional
 *     .withContext(HttpRequest.class, (request, others) -> {
 *         String token = request.getHeader("Authorization");
 *         return token != null
 *             ? Optional.of(new UserSession(token))
 *             : Optional.empty();
 *     })
 *     .build();
 * }</pre>
 *
 * <h2>Supply Strategies</h2>
 * <ul>
 *   <li><b>Value-based</b>: {@link #withValue(Object)} - Returns a fixed instance</li>
 *   <li><b>Constructor-based</b>: {@link #withConstructor(IConstructorBinder)} - Creates new instances via reflection</li>
 *   <li><b>Context-based</b>: {@link #withContext(Class, IContextualObjectSupply)} - Requires context for creation</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * The builder itself is not thread-safe. The thread safety of the resulting supplier
 * depends on the configured supply strategy.
 * </p>
 *
 * @param <Supplied> the type of object the built supplier will provide
 * @since 2.0.0-ALPHA01
 * @see IObjectSupplierBuilder
 * @see IObjectSupplier
 * @see IContextualObjectSupply
 */
public interface ISupplierBuilder<Supplied> extends IObjectSupplierBuilder<Supplied, IObjectSupplier<Supplied>>{

    /**
     * Configures whether the supplier can return null/empty values.
     *
     * <p>
     * When set to {@code true}, the supplier is allowed to return {@link java.util.Optional#empty()}.
     * When {@code false} (default), the supplier should always return a present Optional,
     * and returning empty may be considered an error condition.
     * </p>
     *
     * @param nullable {@code true} to allow null/empty returns, {@code false} to require values
     * @return this builder instance for method chaining
     */
    ISupplierBuilder<Supplied> nullable(boolean nullable);

    /**
     * Configures the supplier to use context-aware object creation logic.
     *
     * <p>
     * This method transforms the builder to create a contextual supplier that requires
     * the specified context type for object creation. The provided supply logic will
     * be invoked with the matching context at supply time.
     * </p>
     *
     * @param <ContextType> the type of context required for object creation
     * @param contextType the class representing the required context type
     * @param supply the functional supply logic that creates objects using the context
     * @return this builder instance for method chaining
     * @throws DslException if a supply strategy has already been configured, or if the
     *                     context type or supply logic is invalid
     */
    <ContextType> ISupplierBuilder<Supplied> withContext(Class<ContextType> contextType, IContextualObjectSupply<Supplied, ContextType> supply) throws DslException;

    /**
     * Configures the supplier to return a fixed value.
     *
     * <p>
     * The built supplier will always return the specified value wrapped in an Optional.
     * This is the simplest supply strategy, suitable for singleton-like objects or
     * pre-configured instances.
     * </p>
     *
     * @param value the fixed value to be supplied (may be {@code null} if nullable is enabled)
     * @return this builder instance for method chaining
     * @throws DslException if a supply strategy has already been configured, or if the
     *                     value is null when nullable is disabled
     */
    ISupplierBuilder<Supplied> withValue(Supplied value) throws DslException;

    /**
     * Configures the supplier to create instances using the specified constructor.
     *
     * <p>
     * The built supplier will use reflection to instantiate objects via the constructor
     * defined by the provided binder. This enables dynamic object creation with
     * parameterized constructors.
     * </p>
     *
     * @param constructorBinder the constructor binder that defines how to create instances
     * @return this builder instance for method chaining
     * @throws DslException if a supply strategy has already been configured, or if the
     *                     constructor binder is invalid or incompatible
     */
    ISupplierBuilder<Supplied> withConstructor(IConstructorBinder<Supplied> constructorBinder) throws DslException;

}